create table method_metrics( package varchar, file varchar ,class varchar, method varchar , metric varchar, value double,PRIMARY KEY (package,file,class,method, metric));
create table class_metrics( package varchar,  file varchar , class varchar ,metric varchar, value  double,PRIMARY KEY (package,file,class, metric));
create table package_metrics( package varchar, metric varchar, value  double,PRIMARY KEY (package, metric));
create table file_metrics( package varchar,file varchar, metric varchar, value double,PRIMARY KEY (file, metric));
