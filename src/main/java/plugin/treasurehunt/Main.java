package plugin.treasurehunt;
import org.bukkit.Bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.treasurehunt.command.GameStartCommand;


public final class Main extends JavaPlugin{

  @Override
  public void onEnable() {
    GameStartCommand gameStartCommand = new GameStartCommand();
    Bukkit.getPluginManager().registerEvents(gameStartCommand,this);
    getCommand("gameStart").setExecutor(gameStartCommand);
  }

}
