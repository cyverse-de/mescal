FROM clojure
COPY . /usr/src/mescal
COPY ./docker/profiles.clj /root/.lein/profiles.clj
WORKDIR /usr/src/mescal
RUN lein deps
CMD ["lein", "test"]
