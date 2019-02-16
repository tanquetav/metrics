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

	private static String classSql = "INSERT INTO class_metrics (package,file,class,metric,value) VALUES (?,?,?,?,?) ";
	private static String classMaxUpdateSql = "UPDATE class_metrics SET value=max(value,?) WHERE package=? and file=? AND class=? AND metric=?";
	private static String methodSql = "INSERT INTO method_metrics (package,file,class,method,metric,value) VALUES (?,?,?,?,?,?) ";
	private static String pkgCreateSql = "INSERT INTO package_metrics (package,metric,value) VALUES (?,?,0) ";
	private static String fileCreateSql = "INSERT INTO file_metrics (package,file,metric,value) VALUES (?,?,?,0) ";
	private static String pkgUpdateSql = "UPDATE package_metrics SET value=value+? WHERE package=? AND metric=?";
	private static String pkgMaxUpdateSql = "UPDATE file_metrics SET value=max(value,?) WHERE package=? and  metric=?";
	private static String fileUpdateSql = "UPDATE file_metrics SET value=value+? WHERE package=? and file=? AND metric=?";
	private static String fileMaxUpdateSql = "UPDATE file_metrics SET value=max(value,?) WHERE package=? and file=? AND metric=?";

	private static String CountParams = "CountParams";
	private static String MaxCyclomatic= "MaxCyclomatic";
	private static String CyclomaticModified = "CyclomaticModified";
	private static String MaxCyclomaticModified = "MaxCyclomaticModified";
	private static String Cyclomatic = "Cyclomatic";
	private static String CountLineCode = "CountLineCode";
	private static String CountDeclMethod = "CountDeclMethod";
	private static String CountDeclClass= "CountDeclClass";
	private static String CountDeclFunction= "CountDeclFunction";

	private String[] metrics = new String [] {CyclomaticModified,Cyclomatic,CountLineCode ,MaxCyclomatic ,MaxCyclomaticModified};

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

				for (String met : metrics ) {
					try (PreparedStatement pstmt = conn.prepareStatement(pkgCreateSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, met);
						pstmt.executeUpdate();
					}
					catch (Exception e ) {
					}
					try (PreparedStatement pstmt = conn.prepareStatement(fileCreateSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, cc.getSourceFileName());
						pstmt.setString(3, met);
						pstmt.executeUpdate();
					}
					catch (Exception e ) {
					}
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileCreateSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, CountDeclClass);
					pstmt.executeUpdate();
				}
				catch (Exception e ) {
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileCreateSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, CountDeclMethod);
					pstmt.executeUpdate();
				}
				catch (Exception e ) {
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileCreateSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, CountDeclFunction);
					pstmt.executeUpdate();
				}
				catch (Exception e ) {
				}

				try (PreparedStatement pstmt = conn.prepareStatement(pkgUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, CyclomaticModified);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(pkgUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, Cyclomatic);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(pkgUpdateSql)) {
					pstmt.setDouble(1, cc.getLineCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, CountLineCode);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(pkgMaxUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, MaxCyclomatic);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(pkgMaxUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, MaxCyclomaticModified);
					pstmt.executeUpdate();
				}


				try (PreparedStatement pstmt = conn.prepareStatement(fileUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, cc.getSourceFileName());
					pstmt.setString(4, CyclomaticModified);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, cc.getSourceFileName());
					pstmt.setString(4, Cyclomatic);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileUpdateSql)) {
					pstmt.setDouble(1, cc.getLineCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, cc.getSourceFileName());
					pstmt.setString(4, CountLineCode);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileUpdateSql)) {
					pstmt.setDouble(1, 1.0);
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, cc.getSourceFileName());
					pstmt.setString(4, CountDeclClass);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileMaxUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, cc.getSourceFileName());
					pstmt.setString(4, MaxCyclomatic);
					pstmt.executeUpdate();
				}
				try (PreparedStatement pstmt = conn.prepareStatement(fileMaxUpdateSql)) {
					pstmt.setDouble(1, cc.getComplexityCounter().getTotalCount());
					pstmt.setString(2, cc.getPackageName());
					pstmt.setString(3, cc.getSourceFileName());
					pstmt.setString(4, MaxCyclomaticModified);
					pstmt.executeUpdate();
				}
 				try (PreparedStatement pstmt = conn.prepareStatement(classSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, cc.getName());
					pstmt.setString(4, CyclomaticModified);
					pstmt.setDouble(5, cc.getComplexityCounter().getTotalCount());
					pstmt.executeUpdate();
				}
 				try (PreparedStatement pstmt = conn.prepareStatement(classSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, cc.getName());
					pstmt.setString(4, Cyclomatic);
					pstmt.setDouble(5, cc.getComplexityCounter().getTotalCount());
					pstmt.executeUpdate();
				}
 				try (PreparedStatement pstmt = conn.prepareStatement(classSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, cc.getName());
					pstmt.setString(4, CountLineCode);
					pstmt.setDouble(5, cc.getLineCounter().getTotalCount());
					pstmt.executeUpdate();
				}
 				try (PreparedStatement pstmt = conn.prepareStatement(classSql)) {
					pstmt.setString(1, cc.getPackageName());
					pstmt.setString(2, cc.getSourceFileName());
					pstmt.setString(3, cc.getName());
					pstmt.setString(4, CountDeclMethod);
					pstmt.setDouble(5, cc.getMethodCounter().getTotalCount());
					pstmt.executeUpdate();
				}

				for (IMethodCoverage mc : cc.getMethods()) {
					try (PreparedStatement pstmt = conn.prepareStatement(fileUpdateSql)) {
						pstmt.setDouble(1, 1);
						pstmt.setString(2, cc.getPackageName());
						pstmt.setString(3, cc.getSourceFileName());
						pstmt.setString(4, CountDeclFunction);
						pstmt.executeUpdate();
					}

					try (PreparedStatement pstmt = conn.prepareStatement(methodSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, cc.getSourceFileName());
						pstmt.setString(3, cc.getName());
						pstmt.setString(4, mc.getName()+" "+mc.getDesc());
						pstmt.setString(5, CyclomaticModified);
						pstmt.setDouble(6, mc.getComplexityCounter().getTotalCount());
						pstmt.executeUpdate();
					}
					try (PreparedStatement pstmt = conn.prepareStatement(methodSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, cc.getSourceFileName());
						pstmt.setString(3, cc.getName());
						pstmt.setString(4, mc.getName()+" "+mc.getDesc());
						pstmt.setString(5, Cyclomatic);
						pstmt.setDouble(6, mc.getComplexityCounter().getTotalCount());
						pstmt.executeUpdate();
					}
					try (PreparedStatement pstmt = conn.prepareStatement(methodSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, cc.getSourceFileName());
						pstmt.setString(3, cc.getName());
						pstmt.setString(4, mc.getName()+" "+mc.getDesc());
						pstmt.setString(5, CountLineCode);
						pstmt.setDouble(6, mc.getLineCounter().getTotalCount());
						pstmt.executeUpdate();
					}
					try (PreparedStatement pstmt = conn.prepareStatement(methodSql)) {
						pstmt.setString(1, cc.getPackageName());
						pstmt.setString(2, cc.getSourceFileName());
						pstmt.setString(3, cc.getName());
						pstmt.setString(4, mc.getName()+" "+mc.getDesc());
						pstmt.setString(5, CountParams);
						pstmt.setDouble(6, getMethodParamCount(mc.getDesc()));
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
