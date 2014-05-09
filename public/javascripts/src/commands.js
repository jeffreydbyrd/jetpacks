function Commands() {
  this.jump = "UP";
  this.down = "DOWN";
  this.left = "LEFT";
  this.right = "RIGHT";
  this.quit = "QUIT";
  this.activate = "ACTIVATE";

  this.keyBindings = {
    32:this.jump, 38:this.jump, 87:this.jump,
    65:this.left, 37:this.left,
    68:this.right, 39:this.right,
    40:this.down, 83:this.down,
    13:this.activate, 69:this.activate,
    81:this.quit
  };
}

var COMMANDS = new Commands();
