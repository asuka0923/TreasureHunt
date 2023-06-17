package plugin.treasurehunt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.treasurehunt.command.GameStartCommand;

public class ChestDetail {
  private static List<Block> arrayChest;
  public static List<Location> chestLocationList;
  public static List<Material> oreList;

  /**
   * プレイヤーの座標を取得し、チェストを等間隔で配置
   * @param player　プレイヤー
   */
  public static void arrangementChest(Player player) {

    arrayChest = new ArrayList<>();
    chestLocationList = new ArrayList<>();

    for (int x = GameLocationDate.areaStartX; x <= GameLocationDate.areaEndX; x += 2)
      for (int z = GameLocationDate.areaStartZ; z <= GameLocationDate.areaEndZ; z += 2) {
        Block block = player.getWorld().getBlockAt(x, GameLocationDate.y, z);
        block.setType(Material.AIR);//二重置き防止
        block.setType(Material.CHEST);
        arrayChest.add(block);
        Location chestLocation = block.getLocation();
        chestLocationList.add(chestLocation);
      }
  }

  /**
   * チェストにランダムに鉱石を格納（レベル別）
   * @param difficulty 難易度
   */
  public static void putOreRandom(String difficulty) {
    GameStartCommand.nowExecutingPlayer.setDifficulty(difficulty);
    oreList = new ArrayList<>();
    oreList.addAll(Arrays.asList(Material.DIAMOND, Material.EMERALD));
    switch (difficulty) {
      case GameStartCommand.EASY ->  {
        addMaterialsToOreList(8, Material.LAPIS_LAZULI);
        addMaterialsToOreList(10, Material.COAL);
        addMaterialsToOreList(5, Material.BONE);
      }
      case GameStartCommand.NORMAL -> {
        addMaterialsToOreList(5, Material.LAPIS_LAZULI);
        addMaterialsToOreList(9, Material.COAL);
        addMaterialsToOreList(9, Material.BONE);
      }
      case GameStartCommand.HARD -> {
        addMaterialsToOreList(2, Material.LAPIS_LAZULI);
        addMaterialsToOreList(8, Material.COAL);
        addMaterialsToOreList(13, Material.BONE);
      }
    }
    Collections.shuffle(oreList);

    oreList.stream()
        .takeWhile(ore -> arrayChest.size() != 0)
        .map(ItemStack::new)
        .forEach(oreStack -> {Block treasureChest = arrayChest.remove(0);
          Inventory inventory = ((Chest) treasureChest.getState()).getInventory();
          inventory.addItem(oreStack);
        });
  }

  /**
   *それぞれの鉱石をレベル別で決められた数入れていく
   * @param count　レベル別に決められた数
   * @param material　鉱石の種類
   */
  private static void addMaterialsToOreList(int count, Material material) {
    IntStream.range(0, count).mapToObj(i -> material).forEach(oreList::add);
  }
  public static void chestReset(Player player){
    if (ChestDetail.chestLocationList  != null) {
      ChestDetail.chestLocationList .stream()
              .map(Location::getBlock)
              .forEach(chestBlock -> chestBlock.setType(Material.AIR));
      GameStartCommand.hasChest = false;
    }
  }

}
