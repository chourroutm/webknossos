### define
underscore : _
app : app
backbone.marionette : marionette
###

class PaginationView extends Backbone.Marionette.ItemView

  template : _.template("""
    <div class="pagination">
      <ul>
        <li class="first <% if (Pagination.currentPage == 1) { %> disabled <% } %>">
          <a href="#"><i class="fa fa-angle-double-left"></i></a>
        </li>
        <li class="prev <% if (Pagination.currentPage == 1) { %> disabled <% } %>"">
          <a href="#"><i class="fa fa-angle-left"></i></a>
        </li>
        <% if (Pagination.lastPage == 1){ %>
          <li>
            <span class="page selected"><%= 1 %></span>
          <li>
        <% } %>
        <% _.each (Pagination.pageSet, function (p) { %>
          <% if (Pagination.currentPage == p) { %>
            <li>
              <span class="page selected"><%= p %></span>
            </li>
          <% } else { %>
            <li>
              <a href="#" class="page"><%= p %></a>
            </li>
          <% } %>
        <% }); %>
        <li class="next <% if (Pagination.currentPage >= Pagination.lastPage) { %> disabled <% } %>">
          <a href="#"><i class="fa fa-angle-right"></i></a>
        </li>
        <li class="last <% if (Pagination.currentPage >= Pagination.lastPage) { %> disabled <% } %>">
          <a href="#"><i class="fa fa-angle-double-right"></i></a>
        </li>
      </ul>
      <input type="text" class="search-query" placeholder="Search">
    </div>
  """)

  className : "container wide"
  templateHelpers :
    Pagination : {}

  ui :
    "inputSearch" : ".search-query"

  events :
    "click .prev" : "goBack"
    "click .next" : "goNext"
    "click .last" : "goLast"
    "click .first" : "goFirst"
    "click .page" : "goToPage"
    "input input" : "filter"


  initialize : ->

   @listenTo(@collection, "reset", @collectionSynced)


  goFirst : ->

    @collection.goTo(1)


  goLast : ->

    @collection.goTo(@collection.totalPages)


  goBack : ->

    @collection.prevPage()


  goNext : ->

    @collection.nextPage()


  gotoPage : (evt) ->

    evt.preventDefault()
    page = $(evt.target).text()
    @collection.goTo(page)


  filter : (evt) ->

    # implement actually filtering on the collection in respective view
    # in order to set correct fields for filtering
    filterQuery = @ui.inputSearch.val()
    app.vent.trigger("paginationView:filter", filterQuery)

    @ui.inputSearch.val(filterQuery)
    @ui.inputSearch.focus()


  collectionSynced : ->

    @templateHelpers.Pagination = @collection.info()
    @render()

