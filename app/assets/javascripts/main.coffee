$             = require("jquery")
_             = require("lodash")
Backbone      = require("backbone")
ErrorHandling = require("libs/error_handling")
Request       = require("libs/request")
app           = require("./app")
require("bootstrap")
require("fetch")
require("promise")
require("./libs/core_ext")

ErrorHandling.initialize( throwAssertions: false, sendLocalErrors: false )

Router = require("./router")

app.addInitializer( ->
  app.router = new Router()
  Backbone.history.start( pushState : true )
)

app.addInitializer( ->
  Request.receiveJSON("/api/user")
    .then((user) ->
      app.currentUser = user
      ErrorHandling.setCurrentUser(user)
      return
    )
)


$ ->
  # show the bootstrap flash modal on load
  $("#flashModal").modal("show")

  app.start()

