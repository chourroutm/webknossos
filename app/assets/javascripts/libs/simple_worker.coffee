### define ###

# `SimpleWorker` is a wrapper around the WebWorker API. First you
# initialize it providing url of the javascript worker code. Afterwards
# you can request work using `send` and wait for the result using the
# returned deferred.
class SimpleWorker

  constructor : (url) ->
    @worker = new Worker(url)

    @worker.onerror = (err) -> 
      console?.error(err)
  
  # Returns a `$.Deferred` object representing the completion state.
  send : (data) ->  
    
    deferred = $.Deferred()

    workerHandle = data.workerHandle = Math.random()

    workerMessageCallback = (event) =>
      
      if (result = event.data).workerHandle == workerHandle
        @worker.removeEventListener("message", workerMessageCallback, false)
        if err = result.err
          deferred.reject(err)
        else 
          deferred.resolve(result)

    @worker.addEventListener("message", workerMessageCallback, false)
    @worker.postMessage(data)

    deferred.promise()

class SimpleWorker.Pool

  constructor : (@url, @workerLimit = 3) ->
    @queue = []
    @workers = []

  send : (data) ->

    for _worker in @workers when not _worker.busy
      worker = _worker
      break
    
    if not worker and @workers.length < @workerLimit
      worker = @spawnWorker()
    
    if worker
      worker.send(data)
    else
      @queuePush(data)
      

  spawnWorker : ->

    worker = new SimpleWorker(@url)
    worker.busy = false
    
    workerReset = =>
      worker.busy = false
      @queueShift(worker)

    worker.worker.onerror = (err) -> 
      console?.error(err)
      workerReset()

    worker.worker.addEventListener("message", workerReset, false)

    @workers.push(worker)

    worker
  
  queueShift : (worker) ->

    if @queue.length > 0 and not worker.busy
      { data, deferred } = @queue.shift()
      worker.send(data)
        .done (data) -> deferred.resolve(data)
        .fail (err) -> deferred.reject(err)
    
  queuePush : (data) ->

    deferred = $.Deferred()
    @queue.push { data, deferred }

SimpleWorker