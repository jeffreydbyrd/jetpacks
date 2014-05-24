(function(){
  var username = function() {
    var hash = window.location.hash;
    var i = hash.indexOf("username=") + 9
    return hash.substring(i);
  }();

  var address = function() {
    var href = window.location.href;
    var addr = href.substring(7, href.indexOf("/#"));
    return "ws://" + addr + "/play?username=" + username;
  }();

  var internal_dimensions = 50;
  var canvasy = UTIL.screenh() / 1.03;
  var canvasx = UTIL.screenw() / 1.03;
  var ky = canvasy / internal_dimensions;
  var kx = canvasx / internal_dimensions;

  var game = new Game(canvasy, canvasy, ky, ky)
  var conn = new Connection(address);

  conn.onReceive("quit", function(params) {
    conn.close();
    document.write("<p>I enjoyed our time together!</p>");
  });

  conn.onReceive("error", function(message) {
    conn.close();
    document.write("<p>Unable to connect: " + message + "</p>");
  });

  bindToKeyboard(conn);
  bindToGame(conn, game);
  conn.start();
})()
