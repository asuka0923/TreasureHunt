package plugin.treasurehunt;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * プレイヤーがゲームを開始した位置を取得し、チェスト配置エリアを決定
 */
public class GameLocationDate {
    public static int areaStartX;
    public static int areaEndX;
    public static int areaStartZ;
    public static int areaEndZ;
    public static int y;

    public static void chestLocation(Player player) {
        Location location = player.getLocation();
        areaStartX = location.getBlockX() - 4;
        areaEndX = location.getBlockX() + 4;
        areaStartZ = location.getBlockZ() + 2;
        areaEndZ = location.getBlockZ() + 10;
        y = location.getBlockY();
    }

    /**
     * プレイヤーがゲームエリア内にいるか確認
     * @param player　プレイヤー
     * @return プレイヤーがゲームエリア内にいるか判定
     */
    public static boolean playerInGameArea(Player player) {
        Location playerLocation = player.getLocation();
        int playerX = playerLocation.getBlockX();
        int playerZ = playerLocation.getBlockZ();
        return playerX >= (GameLocationDate.areaStartX -4) &&
            playerX <= (GameLocationDate.areaEndX +4) &&
            playerZ >= (GameLocationDate.areaStartZ -7) &&
            playerZ <= (GameLocationDate.areaEndZ + 4);

    }
}