#!/bin/sh -eux

# Move to root folder of keycloak
cd ../../../../

#run tests
if [[ ${IP_FAMILY} == "ipv4" || ${IP_FAMILY} == "dual" ]]; then
    DOCKER_REGISTRY=$(hostname --ip-address | grep -oE '\b([0-9]{1,3}\.){3}[0-9]{1,3}\b' | awk '$1 != "127.0.0.1" { print $1 }' | head -1)
elif [[ ${IP_FAMILY} == "ipv6" ]]; then
    DOCKER_REGISTRY="myregistry.local"
fi

export DOCKER_REGISTRY="$DOCKER_REGISTRY:5001"
echo "Using container registry:$DOCKER_REGISTRY"
   
./mvnw install --batch-mode -Poperator -pl :keycloak-operator -am \
    -Dquarkus.kubernetes.image-pull-policy=IfNotPresent \
    -Doperator.keycloak.image=keycloak:$DOCKER_TAG \
    -Dquarkus.container-image.group=$DOCKER_ORG
    -Dquarkus.kubernetes.env.vars.operator-keycloak-image-pull-policy=Never \
    -Dtest.operator.custom.image=custom-keycloak:$DOCKER_TAG \
    -Dquarkus.container-image.registry=$DOCKER_REGISTRY
    --no-transfer-progress -Dtest.operator.deployment=remote
 