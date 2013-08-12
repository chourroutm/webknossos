### define
jquery : $
../model/dimensions : Dimensions
../../libs/toast : Toast
../../libs/event_mixin : EventMixin
../../libs/Tween : TWEEN_LIB
../constants : constants
./modal : modal
###

class PlaneView

  constructor : (@model, @flycam, @stats, @renderer, @scene) ->

    _.extend(@, new EventMixin())

    @running = false

    # The "render" div serves as a container for the canvas, that is 
    # attached to it once a renderer has been initalized.
    container = $("#render")

    # Create a 4x4 grid
    @curWidth = WIDTH = constants.VIEWPORT_WIDTH
    HEIGHT = constants.VIEWPORT_WIDTH
    @scaleFactor = 1

    # Initialize main THREE.js components
    @camera   = new Array(4)
    @lights   = new Array(3)

    for i in constants.ALL_VIEWPORTS
      # Let's set up cameras
      # No need to set any properties, because the camera controller will deal with that
      @camera[i]   = new THREE.OrthographicCamera(0, 0, 0, 0)
      @scene.add @camera[i]

      # There is one light for each plane
      if i != constants.VIEW_3D
        @lights[i]   = new THREE.PointLight( 0xffffff, 0.8 )
        @scene.add @lights[i]

    @camera[constants.PLANE_XY].position.z = -1
    @camera[constants.PLANE_YZ].position.x =  1
    @camera[constants.PLANE_XZ].position.y =  1
    @camera[constants.VIEW_3D].position    = new THREE.Vector3(10, 10, -10)
    @camera[constants.PLANE_XY].up         = new THREE.Vector3( 0, -1,  0)
    @camera[constants.PLANE_YZ].up         = new THREE.Vector3( 0, -1,  0)
    @camera[constants.PLANE_XZ].up         = new THREE.Vector3( 0,  0, -1)
    @camera[constants.VIEW_3D].up          = new THREE.Vector3( 0,  0, -1)
    for cam in @camera
      cam.lookAt(new THREE.Vector3( 0, 0, 0))

    # Because the voxel coordinates do not have a cube shape but are distorted,
    # we need to distort the entire scene to provide an illustration that is
    # proportional to the actual size in nm.
    # For some reason, all objects have to be put into a group object. Changing
    # scene.scale does not have an effect.
    @group = new THREE.Object3D
    # The dimension(s) with the highest resolution will not be distorted
    @group.scale = @model.scaleInfo.getNmPerVoxelVector()
    # Add scene to the group, all Geometries are than added to group
    @scene.add(@group)

    # Attach the canvas to the container
    @renderer.setSize 2*WIDTH+20, 2*HEIGHT+20
    $(@renderer.domElement).attr("id": "render-canvas")
    container.append @renderer.domElement

    @setActivePlaneXY()

    @positionStats = $("#status")

    @first = true
    @newTextures = [true, true, true, true]
    # start the rendering loop


  animate : ->

    return unless @running

    @renderFunction()

    window.requestAnimationFrame => @animate()

  renderFunction : ->
    # This is the main render function.
    # All 3D meshes and the trianglesplane are rendered here.

    TWEEN.update()

    # skip rendering if nothing has changed
    # This prevents you the GPU/CPU from constantly
    # working and keeps your lap cool
    # ATTENTION: this limits the FPS to 30 FPS (depending on the keypress update frequence)

    modelChanged = false
    for plane in @model.binary.planes
      modelChanged |= plane.hasChanged()

    if @flycam.hasChanged or @flycam.hasNewTextures() or modelChanged

      @trigger "render"
      
      # update postion and FPS displays
      @stats.update()
      
      viewport = [[0, @curWidth+20], [@curWidth+20, @curWidth+20], [0, 0], [@curWidth+20, 0]]
      @renderer.autoClear = true
      
      for i in constants.ALL_VIEWPORTS
        @trigger "renderCam", i
        @renderer.setViewport(viewport[i][0], viewport[i][1], @curWidth, @curWidth)
        @renderer.setScissor(viewport[i][0], viewport[i][1], @curWidth, @curWidth)
        @renderer.enableScissorTest(true)
        @renderer.setClearColorHex(constants.PLANE_COLORS[i], 1);
        @renderer.render @scene, @camera[i]
    
      @flycam.hasChanged = false
      @flycam.hasNewTexture = [false, false, false]

      @trigger "finishedRender"
  
  addGeometry : (geometry) ->
    # Adds a new Three.js geometry to the scene.
    # This provides the public interface to the GeometryFactory.

    @group.add geometry


  removeGeometry : (geometry) ->

    @group.remove geometry


  draw : ->

    #Apply a single draw
    @flycam.update()


  resizeThrottled : ->

    # throttle resize to avoid annoying flickering
    @resizeThrottled = _.throttle(
      => @resize()
      constants.RESIZE_THROTTLE_TIME
    )
    @resizeThrottled()


  resize : ->

    #Call this after the canvas was resized to fix the viewport
    canvas = $("#render-canvas")
    WIDTH = (canvas.width()-20)/2
    HEIGHT = (canvas.height()-20)/2

    @renderer.setSize( 2*WIDTH+20, 2*HEIGHT+20)
    for i in constants.ALL_VIEWPORTS
      @camera[i].aspect = WIDTH / HEIGHT
      @camera[i].updateProjectionMatrix()
    @draw()
  

  scaleTrianglesPlane : (scale) =>

    @scaleFactor = scale
    @curWidth = WIDTH = HEIGHT = Math.round(@scaleFactor * constants.VIEWPORT_WIDTH)
    canvas = $("#render-canvas")
    canvas.width(2 * WIDTH + 20)
    canvas.height(2 * HEIGHT + 20)

    $('#prevControls button').width(@curWidth/5)

    divs = $(".inputcatcher")
    for div in divs
      $(div).css({width: WIDTH + "px"})
      $(div).css({height: HEIGHT + "px"})

    @resizeThrottled()


  setActivePlaneXY : =>

    @setActivePlane constants.PLANE_XY


  setActivePlaneYZ : =>

    @setActivePlane constants.PLANE_YZ


  setActivePlaneXZ : =>

    @setActivePlane constants.PLANE_XZ


  setActivePlane : (planeID) =>

    @flycam.setActivePlane planeID
    for i in [0..2]
      $(".inputcatcher")[i].style.borderWidth = if i==planeID then "2px" else "0px"
    @flycam.update()


  getCameras : =>

    @camera


  getLights  : =>

    @lights


  showFirstVisToggle : ->

    modal.show("You just toggled the skeleton visibility. To toggle back, just hit the 1-Key.",
      [{id: "ok-button", label: "OK, Got it."}])


  showBranchModal : (callback) ->

    modal.show("You didn't add a node after jumping to this branchpoint, do you really want to jump again?",
      [{id: "jump-button", label: "Jump again", callback: callback},
       {id: "cancel-button", label: "Cancel"}])


  bind : ->

    @model.cellTracing.on({
      doubleBranch         : (callback) => @showBranchModal(callback)      
      mergeDifferentTrees  : ->
        Toast.error("You can't merge nodes within the same tree", false) })

    @model.user.on 
      scaleChanged : (scale) => if @running then @scaleTrianglesPlane(scale)
    

  stop : ->

    $(".inputcatcher").hide()

    $(window).off "resize", => @.resize()

    @running = false 


  start : ->

    @running = true

    @scaleTrianglesPlane(@model.user.scale)
    $(".inputcatcher").show()

    $(window).on "resize", => @.resize()

    @animate()

