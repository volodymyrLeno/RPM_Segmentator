# Several things (no docker image or debian package versions specified, no multistage to get rid of the source, root user, git show, ...) about this dockerfile are very bad style. Consider it a demonstration...

FROM debian
RUN apt-get update && apt-get install -y maven wget git && rm -rf /var/lib/{dpkg,apt} /var/cache/debconf

RUN wget http://code.deckfour.org/xes/OpenXES-1.1.tar.gz \
	&& echo '8913816c480fc3bc198144fa9936044ef661bb4a1d8aa150f1ef94eb2bcc83c7  OpenXES-1.1.tar.gz' | sha256sum -c \
	&& tar xvf OpenXES-1.1.tar.gz --strip-components=2 \
   	&& mvn install:install-file -Dfile=OpenXES.jar -DgroupId=org.deckfour.xes -DartifactId=openxes -Dversion=1.1 -Dpackaging=jar -DgeneratePom=true \
	&& rm -rf OpenXES-1.1.tar.gz OpenXES.jar

WORKDIR /rpm-segmentator
COPY .git ./.git
COPY pom.xml .
COPY src ./src
# https://sep.cs.ut.ee/uploads/Main/bpstruct.zip doesn't seem to contain the right jar…?
RUN git show 208f4ad75ac206f1fe1a48699814b86a000b90a0:src/bpstruct-0.1.117.jar >bpstruct-0.1.117.jar \
	&& mvn install:install-file -Dfile=bpstruct-0.1.117.jar   -DgroupId=ee.ut.bpstruct -DartifactId=bpstruct -Dversion=0.1.117 -Dpackaging=jar -DgeneratePom=true \
	&& rm bpstruct-0.1.117.jar

RUN sed -ri '/<dependencies>/ a \
	<dependency><groupId>org.deckfour.xes</groupId><artifactId>openxes</artifactId><version>1.1</version></dependency> \
	<dependency><groupId>ee.ut.bpstruct</groupId><artifactId>bpstruct</artifactId><version>0.1.117</version></dependency>' pom.xml \
	&& mvn package

ENTRYPOINT ["/usr/bin/java", "-jar", "/rpm-segmentator/target/RPM_Segmentator-1.0-SNAPSHOT-jar-with-dependencies.jar"]

# Try:
# docker build -t rpm-segmentator .
# docker run -ti --rm -v $PWD/out/artifacts/RPM_Segmentator_jar:/data -w /data rpm-segmentator logs/StudentRecord.csv config.json null
