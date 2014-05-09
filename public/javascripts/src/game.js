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

  function convertXPos(x) { return x * kx }
  function convertYPos(y){ return canvasY - (y * ky) }

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

  self.bindTo = function(conn) {
    conn.onReceive("create", function(params) {
      self.create(params.id,
                  convertXPos(params.position[0]),
                  convertYPos(params.position[1]),
                  params.dimensions[0] * kx,
                  params.dimensions[1] * ky);
    });

    conn.onReceive("update-positions", function(params) {
      for(id in params) {
        self.move(id,
                  convertXPos(params[id][0]),
                  convertYPos(params[id][1]) );
      }
    });

    conn.onReceive("update-intro", function(params){
      self.updateIntro(params);
    });

    conn.onReceive("quit", function(params) {
      conn.close();
      document.write("<p>I enjoyed our time together!</p>");
    });

    conn.onReceive("error", function(message) {
      conn.close();
      document.write("<p>Unable to connect: " + message + "</p>");
    });
  };
}
