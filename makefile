

all: compile package

compile:
	mvn compile

package:
	mvn package

run:
	java -Dconfig.file=postg.file -jar target/jumpstart-jar-with-dependencies.jar


clean:
	-rm -r target
	-rm -r lib
	-rm -r log

