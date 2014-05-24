function bindToGame(conn, game) {
  conn.onReceive("gameplay-start", function() {
    game.startGamePlay();
  });

  conn.onReceive("create", function(params) {
    game.create(params.id,
                game.convertXPos(params.position[0]),
                game.convertYPos(params.position[1]),
                game.convertWidth(params.dimensions[0]),
                game.convertHeight(params.dimensions[1]));
  });

  conn.onReceive("update-positions", function(params) {
    for(id in params) {
      game.move(id,
                game.convertXPos(params[id][0]),
                game.convertYPos(params[id][1]) );
    }
  });

  conn.onReceive("update-intro", function(params){
    game.updateIntro(params);
  });
}
