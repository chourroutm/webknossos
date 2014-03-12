### define 
./model/binary : Binary
./model/skeletontracing/skeletontracing : SkeletonTracing
./model/user : User
./model/volumetracing/volumetracing : VolumeTracing
./model/scaleinfo : ScaleInfo
./model/flycam2d : Flycam2d
./model/flycam3d : Flycam3d
libs/request : Request
libs/toast : Toast
./constants : constants
###

# This is the model. It takes care of the data including the 
# communication with the server.

# All public operations are **asynchronous**. We return a promise
# which you can react on.

class Model


  timestamps : []
  buckets : []
  bytes : []
  totalBuckets : []
  totalBytes : []


  logConnectionInfo : =>

    @timestamps.push(new Date().getTime())

    bytes = 0
    buckets = 0
    totalBytes = 0
    totalBuckets = 0

    for dataLayerName of @binary
      bytes += @binary[dataLayerName].pullQueue.loadedBytes
      buckets += @binary[dataLayerName].pullQueue.loadedBuckets
      totalBytes += @binary[dataLayerName].pullQueue.totalLoadedBytes
      totalBuckets += @binary[dataLayerName].pullQueue.totalLoadedBuckets
      @binary[dataLayerName].pullQueue.loadedBytes = 0
      @binary[dataLayerName].pullQueue.loadedBuckets = 0

    @bytes.push(bytes)
    @buckets.push(buckets)
    @totalBytes.push(totalBytes)
    @totalBuckets.push(totalBuckets)


  initialize : (controlMode) =>

    tracingId = $("#container").data("tracing-id")
    tracingType = $("#container").data("tracing-type")

    Request.send(
      url : "/annotations/#{tracingType}/#{tracingId}/info"
      dataType : "json"
    ).pipe (tracing) =>

      if tracing.error
        Toast.error(tracing.error)
        {"error": true}

      else unless tracing.content.dataSet
        Toast.error("Selected dataset doesnt exist")
        {"error": true}

      else
        Request.send(
          url : "/user/configuration"
          dataType : "json"
        ).pipe(
          (user) =>

            $.assertExtendContext({
              task: tracingId
              dataSet: tracing.content.dataSet.name
            })

            console.log "tracing", tracing
            console.log "user", user

            dataSet = tracing.content.dataSet
            @user = new User(user)
            @scaleInfo = new ScaleInfo(dataSet.scale)

            if (bb = tracing.content.boundingBox)?
                @boundingBox = {
                  min : bb.topLeft
                  max : [
                    bb.topLeft[0] + bb.width
                    bb.topLeft[1] + bb.height
                    bb.topLeft[2] + bb.depth
                  ]
                }

            layerOptions = {
              color : { allowManipulation : true },
              volume : { allowManipulation : false },
              segmentation : { allowManipulation : false }
            }

            zoomStepCount = Infinity
            @binary = {}
            for layer in dataSet.dataLayers
              _.extend layer, layerOptions[layer.name]
              layer.bitDepth = parseInt( layer.elementClass.substring(4) )
              @binary[layer.name] = new Binary(@user, tracing, layer, tracingId, @boundingBox)
              zoomStepCount = Math.min(zoomStepCount, @binary[layer.name].cube.ZOOM_STEP_COUNT - 1)

            unless @binary["color"]?
              Toast.error("No data available! Something seems to be wrong with the dataset.")

            # if "volume" layer still used, change name to segmentation
            if @binary["volume"]?
              @binary["segmentation"] = @binary["volume"]
              delete @binary["volume"]

            @flycam = new Flycam2d(constants.PLANE_WIDTH, @scaleInfo, zoomStepCount, @user)      
            @flycam3d = new Flycam3d(constants.DISTANCE_3D, dataSet.scale)

            @flycam3d.on
              "changed" : (matrix) =>
                @flycam.setPosition([matrix[12], matrix[13], matrix[14]])
            @flycam.on
              "positionChanged" : (position) =>
                @flycam3d.setPositionSilent(position)

            @flycam.setPosition(tracing.content.editPosition)
            
            if controlMode == constants.CONTROL_MODE_TRACE

              if "volume" in tracing.content.settings.allowedModes
                $.assert( @binary["segmentation"]?,
                  "Volume is allowed, but segmentation does not exist" )
                @volumeTracing = new VolumeTracing(tracing, @flycam, @binary["segmentation"].cube)
              
              else
                @skeletonTracing = new SkeletonTracing(tracing, @scaleInfo, @flycam, @flycam3d, @user)
            
            {"restrictions": tracing.restrictions, "settings": tracing.content.settings}
            
          -> Toast.error("Ooops. We couldn't communicate with our mother ship. Please try to reload this page.")
        )