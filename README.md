Buildar com gradle jar

gerar base com:

java -jar build/libs/metrics.jar build/libs/ /path/doprojeto/java/compilaod  /tmp/saida.db

usar o fork do srcheck:

https://github.com/tanquetav/srccheck  

, branch feature/metrics

para processar esse saida db. mudei o projeto para ter o flag --dbin para apontar para esse db. Ta funcionando o scatterplot, histograma e validacao inicial.
