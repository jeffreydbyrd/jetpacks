function Game(canvasX, canvasY, kx, ky) {
  var self = this;

  var game =
    new Phaser.Game(canvasX, canvasY,
                    Phaser.AUTO, '',
                    { preload: preload, create: create });

  function preload() {
    game.stage.backgroundColor = '#FFFFFF';
    game.load.image('black', '/assets/images/black.png');

  }

  function create() { }

  function convertXPos(x) { return x * kx }
  function convertYPos(y){ return canvasY - (y * ky) }

  var entities = {};

  self.create = function(id, x, y, w, h) {
    var sprite = game.add.tileSprite(x, y, w, h, 'black');
    sprite.anchor.set(0.5);
    entities[id] = sprite;
  };

  self.move = function(id, x, y) {
    game.add.tween(entities[id]).to({x:x, y:y}, 15, Phaser.Easing.Linear.None, true);
  };

  self.bindTo = function(conn) {
    conn.onReceive("create", function(params) {
      self.create(params.id,
                  convertXPos(params.position[0]),
                  convertYPos(params.position[1]),
                  params.dimensions[0] * kx,
                  params.dimensions[1] * ky);
    });

    conn.onReceive("update_positions", function(params) {
      for(id in params) {
        self.move(id,
                  convertXPos(params[id][0]),
                  convertYPos(params[id][1]) );
      }
    });

    conn.onReceive("quit", function(params) {
      conn.close();
      document.write("<p>later!</p>");
    });

  };
}
