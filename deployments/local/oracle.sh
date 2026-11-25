
  podman run -it --rm  \
    --name oracle \
    -p 1521:1521 \
    -e ORACLE_PASSWORD=CHANGEME gvenzl/oracle-free:latest