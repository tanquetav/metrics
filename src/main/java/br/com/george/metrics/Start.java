/*******************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package br.com.george.metrics;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example usage of the JaCoCo core API. In this tutorial a single target class
 * will be instrumented and executed. Finally the coverage information will be
 * dumped.
 */
public final class Start {

	private static String classSql = "INSERT INTO class_metrics (package,file,class,cyclomatic,cyclomaticmodified,lineofcode,methods) VALUES (?,?,?,?,?,?,?) ";
	private static String methodSql = "INSERT INTO method_metrics (package,file,class,method,cyclomatic,cyclomaticmodified,lineofcode,countparams) VALUES (?,?,?,?,?,?,?,?) ";
	private static String pkgCreateSql = "INSERT INTO package_metrics (package,cyclomatic,cyclomaticmodified,lineofcode) VALUES (?,0,0,0) ";
	private static String fileCreateSql = "INSERT INTO file_metrics (file,cyclomatic,cyclomaticmodified,lineofcode) VALUES (?,0,0,0) ";
	private static String pkgUpdateSql = "UPDATE package_metrics SET cyclomatic=cyclomatic+?,cyclomaticmodified=cyclomaticmodified+?,lineofcode=lineofcode+? WHERE package=? ";
	private static String fileUpdateSql = "UPDATE file_metrics SET cyclomatic=cyclomatic+?,cyclomaticmodified=cyclomaticmodified+?,lineofcode=lineofcode+? WHERE file=? ";


	private static Pattern allParamsPattern = Pattern.compile("(\\(.*?\\))");
	private static Pattern paramsPattern = Pattern.compile("(\\[?)(C|Z|S|I|J|F|D|(:?L[^;]+;))");


	int getMethodParamCount(String methodRefType) {
		Matcher m = allParamsPattern.matcher(methodRefType);
		if (!m.find()) {
			throw new IllegalArgumentException("Method signature does not contain parameters");
		}
		String paramsDescriptor = m.group(1);
		Matcher mParam = paramsPattern.matcher(paramsDescriptor);

		int count = 0;
		while (mParam.find()) {
			count++;
		}
		return count;
	}


	private final PrintStream out;

	/**
	 * Creates a new example instance printing to the given stream.
	 *
	 * @param out
	 *            stream for outputs
	 */
	public Start(final PrintStream out) {
		this.out = out;
	}

	/**
	 * Run this example.
	 * 
	 * @throws Exception
	 *             in case of errors
	 */
	public void execute(String path, String sqlfile) throws Exception {
		final ExecutionDataStore executionData = new ExecutionDataStore();
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);


		new File(sqlfile).delete();
		Class.forName("org.sqlite.JDBC");

		Connection conn = null;
		try {
			String url = "jdbc:sqlite:"+sqlfile ;
			conn = DriverManager.getConnection(url);
			Statement st = conn.createStatement();


			System.out.println("Connection to SQLite has been established.");
			try (InputStream is = getClass().getClassLoader().getResourceAsStream("script.sql") ) {
				List<String> lines = IOUtils.readLines(is, "UTF-8");
				for (String line : lines) {
					st.execute(line);
				}
			}
			String[] extensions = { "class" };
			boolean recursive = true;

			Collection<File> files = FileUtils.listFiles(new File(path), extensions, recursive);

			for (Iterator iterator = files.iterator(); iterator.hasNext();) {
				File file = (File) iterator.next();
				this.out.println("File = " + file.getAbsolutePath());
				try (InputStream  original = FileUtils.openInputStream(file)) {
					analyzer.analyzeClass(IOUtils.toByteArray(original), file.getAbsolutePath());
				}
			}

			for (final IClassCoverage cc : coverageBuilder.getClasses()) {

				try (PreparedStatement pstmt = conn.prepareStatement(pkgCreateSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.executeUpdate();
				}
				catch (Exception e ) {
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileCreateSql)) {
					pstmt.setString(1, cc.getSourceFileName());
					pstmt.executeUpdate();
				}
				catch (Exception e ) {
				}
				try (PreparedStatement pstmt = conn.prepareStatement(pkgUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setDouble(2, cc.getComplexityCounter().getTotalCount());
					pstmt.setDouble(3, cc.getLineCounter().getTotalCount());
					pstmt.setString(4, cc.getPackageName());
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setDouble(2, cc.getComplexityCounter().getTotalCount());
					pstmt.setDouble(3, cc.getLineCounter().getTotalCount());
					pstmt.setString(4, cc.getSourceFileName());
					pstmt.executeUpdate();
				}

 				try (PreparedStatement pstmt = conn.prepareStatement(classSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, cc.getName());
					pstmt.setDouble(4, cc.getComplexityCounter().getTotalCount());
					pstmt.setDouble(5, cc.getComplexityCounter().getTotalCount());
					pstmt.setDouble(6, cc.getLineCounter().getTotalCount());
					pstmt.setDouble(7, cc.getMethodCounter().getTotalCount());
					pstmt.executeUpdate();
				}

				for (IMethodCoverage mc : cc.getMethods()) {
					try (PreparedStatement pstmt = conn.prepareStatement(methodSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, cc.getSourceFileName());
						pstmt.setString(3, cc.getName());
						pstmt.setString(4, mc.getName()+" "+mc.getDesc());
						pstmt.setDouble(5, mc.getComplexityCounter().getTotalCount());
						pstmt.setDouble(6, mc.getComplexityCounter().getTotalCount());
						pstmt.setDouble(7, mc.getLineCounter().getTotalCount());
						pstmt.setDouble(8, getMethodParamCount(mc.getDesc()));
						pstmt.executeUpdate();
					}
				}

			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
		}


//		final String targetName = TestTarget.class.getName();

//		InputStream  original = getTargetClass(targetName);
//		analyzer.analyzeClass(original, targetName);
//		original.close();

	}

	private InputStream getTargetClass(final String name) {
		final String resource = '/' + name.replace('.', '/') + ".class";
		return getClass().getResourceAsStream(resource);
	}

	private void printCounter(final String unit, final ICounter counter) {
		final Integer missed = Integer.valueOf(counter.getMissedCount());
		final Integer total = Integer.valueOf(counter.getTotalCount());
		out.printf("%s %s %n", total, unit);
	}

	private String getColor(final int status) {
		switch (status) {
		case ICounter.NOT_COVERED:
			return "red";
		case ICounter.PARTLY_COVERED:
			return "yellow";
		case ICounter.FULLY_COVERED:
			return "green";
		}
		return "";
	}

	/**
	 * Entry point to run this examples as a Java application.
	 * 
	 * @param args
	 *            list of program arguments
	 * @throws Exception
	 *             in case of errors
	 */
	public static void main(final String[] args) throws Exception {
		if (args.length!=2) {
			System.out.println("Error start with 2 arguments: java -jar metrics.jar <Path_to_parse> <db_output>");
			return;
		}
		new Start(System.out).execute(args[0],args[1]);
	}

}
