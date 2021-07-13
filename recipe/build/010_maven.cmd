# Build and install jars into local repo
mvn -f {{ctx.cooked_recipe_home('pom.xml')}} clean package install

