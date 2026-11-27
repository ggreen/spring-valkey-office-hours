
cf push product-app  -b java_buildpack_offline -p applications/product-app/build/libs/product-app-0.0.1-SNAPSHOT.jar -f deployments/cloud/cloudFoundry/apps/product-app.yml
