from buildtool.recipe import DefaultRecipe


class Recipe(DefaultRecipe):

    def pre_build(self):
        self.ctx.depends_on(recipe="pipeforce-sdk-java")
