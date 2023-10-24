#!/bin/sh -eux

# Move to root folder of keycloak
cd ../../../../
   
./mvnw install --batch-mode -Poperator -pl :keycloak-operator -am \
    -Dquarkus.kubernetes.image-pull-policy=IfNotPresent \
    -Doperator.keycloak.image=${KEYCLOAK_IMAGE} \
    -Dquarkus.kubernetes.env.vars.operator-keycloak-image-pull-policy=IfNotPresent \
    -Dquarkus.container-image.image=${KEYCLOAK_OPERATOR_IMAGE} \
    -Dtest.operator.custom.image=${CUSTOM_IMAGE} \
    --no-transfer-progress -Dtest.operator.deployment=remote
 
 # TODO: olm tests - will need to move the logic in the gha into a script