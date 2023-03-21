FROM tegonal/jelastic-openjdk:17

LABEL git-commit=$COMMIT_SHORT_SHA

ADD ./target/universal/*.zip artifact.zip

EXPOSE 9000