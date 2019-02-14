create table method_metrics( package varchar, file varchar ,class varchar, method varchar , cyclomatic double, cyclomaticmodified double, lineofcode double, countparams double,PRIMARY KEY (package,file,class,method));
create table class_metrics( package varchar,  file varchar , class varchar ,cyclomatic double, cyclomaticmodified double, lineofcode double, methods double,PRIMARY KEY (package,file,class));
create table package_metrics( package varchar, cyclomatic double, cyclomaticmodified double, lineofcode double,PRIMARY KEY (package));
create table file_metrics( file varchar, cyclomatic double, cyclomaticmodified double, lineofcode double,PRIMARY KEY (file));
