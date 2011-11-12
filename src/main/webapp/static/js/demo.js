var cam, changePerspectiveParams, clipping_distance, keyDown, mesh, mouseDown, mousePressed, mouseReleased, pointcloud, ps, render, setCamPosition, start;
ps = void 0;
pointcloud = void 0;
mesh = void 0;
cam = void 0;
mouseDown = false;
clipping_distance = 15.0;
mousePressed = function() {
  return mouseDown = true;
};
mouseReleased = function() {
  return mouseDown = false;
};
keyDown = function() {
  switch (ps.key) {
    case 119:
      cam.pos = V3.add(cam.pos, V3.scale(cam.dir, 0.2));
      break;
    case 115:
      cam.pos = V3.add(cam.pos, V3.scale(cam.dir, -0.2));
      break;
    case 97:
      cam.pos = V3.add(cam.pos, V3.scale(cam.left, 0.2));
      break;
    case 100:
      cam.pos = V3.add(cam.pos, V3.scale(cam.left, -0.2));
  }
};
render = function() {
  var d, h, length_dir, n0, p, status, versch, y;
  if (mouseDown) {
    y = -(ps.mouseX - ps.width / 2) / ps.width / 45;
    cam.yaw(y);
    h = -(ps.mouseY - ps.height / 2) / ps.height / 8;
    cam.pos = V3.add(cam.pos, [0, h, 0]);
  }
  ps.loadMatrix(M4x4.makeLookAt(cam.pos, V3.add(cam.dir, cam.pos), cam.up));
  length_dir = Math.sqrt(cam.dir[0] * cam.dir[0] + cam.dir[1] * cam.dir[1] + cam.dir[2] * cam.dir[2]);
  n0 = [cam.dir[0] / length_dir, cam.dir[1] / length_dir, cam.dir[2] / length_dir];
  versch = [clipping_distance * n0[0], clipping_distance * n0[1], clipping_distance * n0[2]];
  p = V3.add(cam.pos, versch);
  d = V3.dot(p, n0);
  ps.uniformf("d", d);
  ps.uniformf("n0", n0);
  ps.clear();
  ps.render(pointcloud);
  ps.translate(p[0], p[1], p[2]);
  ps.renderMesh(mesh);
  status = document.getElementById('status');
  status.innerHTML = "" + (Math.floor(ps.frameRate)) + " FPS <br/> " + pointcloud.numPoints + " Points <br />" + cam.pos;
};
start = function() {
  cam = new FreeCam();
  cam.pos = [6, 5, -15];
  ps = new PointStream();
  ps.setup(document.getElementById('render'), {
    "antialias": true
  });
  /*
  	vert = ps.getShaderStr("js/libs/pointstream/shaders/clip.vs")
  	frag = ps.getShaderStr("js/libs/pointstream/shaders/clip.fs")
  	progObj = ps.createProgram(vert, frag);
  	ps.useProgram(progObj);
  	*/
  ps.perspective(60, ps.width / ps.height, 15, 20);
  ps.background([0.9, 0.9, 0.9, 1]);
  ps.pointSize(5);
  ps.onRender = render;
  ps.onMousePressed = mousePressed;
  ps.onMouseReleased = mouseReleased;
  ps.onKeyDown = keyDown;
  pointcloud = read_binary_file();
  mesh = load_obj_file();
};
setCamPosition = function() {
  var x, y, z;
  x = parseFloat(document.getElementById('camX').value);
  y = parseFloat(document.getElementById('camY').value);
  z = parseFloat(document.getElementById('camZ').value);
  if (!isNaN(x) && !isNaN(y) && !isNaN(z)) {
    cam.pos = [x, y, z];
  }
};
changePerspectiveParams = function() {
  var far, fovy, near;
  near = parseFloat(document.getElementById('near').value);
  far = parseFloat(document.getElementById('far').value);
  fovy = parseFloat(document.getElementById('fovy').value);
  if (!isNaN(near) && !isNaN(far) && !isNaN(fovy)) {
    ps.perspective(fovy, ps.width / ps.height, near, far);
  }
};