FROM clojure
COPY ./docker/profiles.clj /root/.lein/profiles.clj
WORKDIR /usr/src/mescal

COPY project.clj /usr/src/mescal/
RUN lein deps

COPY . /usr/src/mescal
CMD ["lein", "test"]
