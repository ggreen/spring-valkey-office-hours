podman run -it --rm --name mysql \
  -e MYSQL_ROOT_PASSWORD=CHANGEME \
  -p 3306:3306 \
   mysql:latest