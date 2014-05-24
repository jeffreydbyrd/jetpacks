function Game(canvasX, canvasY, kx, ky) {
  var self = this;

  var stage = new PIXI.Stage(0xFFFFFF);
  var renderer = PIXI.autoDetectRenderer(canvasX, canvasY);

  document.body.appendChild(renderer.view);

  var entities = {};

  requestAnimFrame( animate );
  function animate() {
    requestAnimFrame( animate );
    renderer.render(stage);
  }

  self.convertXPos = function(x) { return x * kx };
  self.convertYPos = function(y){ return canvasY - (y * ky) };

  self.convertWidth = function(w) {return w * kx};
  self.convertHeight = function(h) {return h * ky};

  var intro = null;
  self.updateIntro = function(statuses) {
    var text = "Waiting on players\nPress enter when ready\n\n";

    for(i in statuses) {
      var status = statuses[i].isReady ? "ready" : "waiting";
      text = text + statuses[i].name + "... " + status + "\n";
    }

    if (intro != null) {
      stage.removeChild(intro);
    }

    intro = new PIXI.Text(text, {font:"24px Arial"});
    stage.addChild(intro);
  };

  self.startGamePlay = function() {
    stage.removeChild(intro);
  };

  self.create = function(id, x, y, w, h) {
    var texture = PIXI.Texture.fromImage("/assets/images/black.png");

    var sprite = new PIXI.Sprite(texture);
    sprite.anchor.x = 0.5;
    sprite.anchor.y = 0.5;
    sprite.position.x = x;
    sprite.position.y = y;
    sprite.width = w;
    sprite.height = h;
    stage.addChild(sprite);

    entities[id] = sprite;
  };

  self.move = function(id, x, y) {
    entities[id].position.x = x;
    entities[id].position.y = y;
  };
}
