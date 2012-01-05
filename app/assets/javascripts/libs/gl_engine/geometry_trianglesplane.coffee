class Trianglesplane extends Geometry
	constructor: (fragmentShader, vertexShader) ->
		super(fragmentShader, vertexShader)
		@vertexIndex = 
			EBO : null
			length : null	
		
		@type = "Trianglesplane"

	setVertexIndex : (data, len) -> 
		@vertexIndex.EBO = data
		@vertexIndex.length = len

	setNormalVertices : (data, width) -> 
		@normalVertices = data
		@normalVerticesWidth = width

	setVertices : (data, len) -> 
		super data, len

	setColors : (data, len) ->
		super data, len

	setNormals : (data, len) ->
		super data, len

	getClassType : ->
		super
