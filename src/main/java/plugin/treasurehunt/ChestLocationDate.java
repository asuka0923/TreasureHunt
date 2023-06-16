package plugin.treasurehunt;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * プレイヤーがゲームを開始した位置を取得し、チェスト配置エリアを決定
 */
public class ChestLocationDate {
    public static int areaStartX;
    public static int areaEndX;
    public static int areaStartZ;
    public static int areaEndZ;
    public static int y;

    public ChestLocationDate(Player player) {
        Location location = player.getLocation();
        this.areaStartX = location.getBlockX() - 4;
        this.areaEndX = location.getBlockX() + 4;
        this.areaStartZ = location.getBlockZ() + 2;
        this.areaEndZ = location.getBlockZ() + 10;
        this.y = location.getBlockY();
    }
}