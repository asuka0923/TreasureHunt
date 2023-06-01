package plugin.treasurehunt.command;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import plugin.treasurehunt.data.PlayerScore;
import plugin.treasurehunt.repository.PlayerScoreRepository;

public class GameStartCommand extends BaseCommand implements Listener {
    private List<int[]> chestIntLocation;
    private List<Location> chestLocationList;
    private boolean hasChest;
    private boolean hasDia;
    private int areaStartX;
    private int areaEndX;
    private int areaStartZ;
    private int areaEndZ;
    public static final String EASY ="easy";
    public static final String NORMAL ="normal";
    public static final String HARD ="hard";
    public static final String NONE ="none";
    public static final String LIST ="list";

    public String difficulty;
    public long startTime;
    private PlayerScore nowPlayerScore = new PlayerScore();
    private List<PlayerScore> playerScoreList = new ArrayList<>();
    @Autowired
    private PlayerScoreRepository playerScoreRepository;

    @Override
    public boolean onExecutePlayerCommand(Player player, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 1 && LIST.equals(args[0])){
            String url =  "jdbc:mysql://localhost:3306/spigot_plugin";
            String user = "root";
            String password = "pass";
            String sql = "select * from player_score;";

            try(Connection con = DriverManager.getConnection(url, user, password);
                Statement statement = con.createStatement();
                ResultSet resultset = statement.executeQuery(sql)) {
                while (resultset.next()) {
                    int id = resultset.getInt("id");
                    Timestamp timestamp = resultset.getTimestamp("date");
                    int score = resultset.getInt("score");
                    int time = resultset.getInt("time");
                    String difficulty = resultset.getString("difficulty");
                    player.sendMessage(id + "|" + timestamp + "|" + score + "|" + time + "|" + difficulty);
                }
            }
                catch (SQLException e){
                    e.printStackTrace();
                }
            return false;
            }
        difficulty = getDifficulty(player,args);
        if (difficulty.equals(NONE)) return false;
        hasChest = true;
        hasDia = true;
        gameplay(player, difficulty);
        return true;
    }
    private void gameplay(Player player,String difficulty){

        if(!hasDia){
            long timeTaken = System.currentTimeMillis() - startTime;
            int secondsNumber = (int) (timeTaken / 1000);
            if(secondsNumber <= 10){
                nowPlayerScore.setPoint(nowPlayerScore.getPoint() +500);
            }else if(secondsNumber <= 20){
                nowPlayerScore.setPoint(nowPlayerScore.getPoint() +100);
            }
            playerScoreList.add(nowPlayerScore);
            player.sendTitle("ゲーム終了！" + secondsNumber + "秒"
                    ,difficulty + "モードでのあなたのスコアは" + nowPlayerScore.getPoint() + "点！");
            PlayerScore score = new PlayerScore();
            score.setPoint(nowPlayerScore.getPoint());
            score.setTime(nowPlayerScore.getTime());
            score.setDifficulty(difficulty);
            playerScoreRepository.save(score);
            gameReset(player);
            return;
        }

        startTime = System.currentTimeMillis();
        Location location = player.getLocation();
        List<Block> chestList = arrangementChest(player, location);
        putOreRandom(chestList, difficulty);

    }

    private String getDifficulty(Player player, @NotNull String[] args) {
        if(args.length == 1 && (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0]))){
            return args[0];
        }
        player.sendMessage("コマンドと半角スペースに続いて、難易度を入力してください(easy,normal,hard)");
        return NONE;
    }
    /**
     *ゲームエリアにチェストを配置してリスト化
     * @param player プレーヤー
     * @param location　ゲームエリア
     * @return チェストリスト
     */
    public List<Block> arrangementChest(Player player, Location location) {
        areaStartX = location.getBlockX() - 4;
        areaEndX = location.getBlockX() + 4;
        areaStartZ = location.getBlockZ() + 2;
        areaEndZ = location.getBlockZ() + 10;
        int y = location.getBlockY();

        List<Block> arrayChest = new ArrayList<>();
        chestIntLocation = new ArrayList<>();
        chestLocationList = new ArrayList<>();

        for (int x = areaStartX; x <= areaEndX; x += 2)
            for (int z = areaStartZ; z <= areaEndZ; z += 2) {
                Block block = player.getWorld().getBlockAt(x, y, z);
                block.setType(Material.AIR);//二重置き防止

                block.setType(Material.CHEST);
                arrayChest.add(block);
                Location chestLocation = block.getLocation();
                chestIntLocation.add(new int[]{chestLocation.getBlockX(), chestLocation.getBlockY(), chestLocation.getBlockZ()});
            }
        for (int[] coords : chestIntLocation) {//チェストの座標リスト
            chestLocationList.add(new Location(player.getWorld(), coords[0], coords[1], coords[2]));
        }

        return arrayChest;
    }



    /**
     *チェストインベントリをクリックしたときにポイント加算
     * @param e チェストインベントリをクリックするイベント
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        //　生成したチェスト以外は対象外
        Player player = (Player) e.getWhoClicked();
        if (chestLocationList == null|| e.getCurrentItem() == null || !chestLocationList.contains(e.getInventory().getLocation())){
            return;}
        // プレイヤーインベントリからチェストインベントリへのアイテム移動を禁止
        if (Objects.requireNonNull(e.getClickedInventory()).getType() == InventoryType.PLAYER) {
            player.sendMessage("アイテムを入れるのは禁止です");
            e.setCancelled(true);
        }
        // クリックされたアイテムがチェスト内のものだったらポイント加算
        if (e.getClickedInventory().getType() == InventoryType.CHEST) {

                switch (e.getCurrentItem().getType()) {
                case EMERALD -> {
                    nowPlayerScore.setPoint(nowPlayerScore.getPoint() +100);
                    player.sendMessage("やった！エメラルドをゲット！現在のポイントは" + nowPlayerScore.getPoint() + "点！");
                }
                case LAPIS_LAZULI -> {
                    nowPlayerScore.setPoint(nowPlayerScore.getPoint() +50);
                    player.sendMessage("ラピスラズリをゲット！現在のポイントは" + nowPlayerScore.getPoint() + "点！");
                }
                case COAL -> {
                    nowPlayerScore.setPoint(nowPlayerScore.getPoint() +20);
                    player.sendMessage("石炭をゲット！現在のポイントは" + nowPlayerScore.getPoint() + "点！");
                }
                case BONE -> {
                    nowPlayerScore.setPoint(nowPlayerScore.getPoint() -5);
                    player.sendMessage("残念、骨だった・・・現在のポイントは" + nowPlayerScore.getPoint() + "点！");
                }
                case DIAMOND -> {
                    nowPlayerScore.setPoint(nowPlayerScore.getPoint() +100);
                    player.sendMessage("ダイヤを見つけたよ！" );
                    hasDia = false;
                    gameplay(player, difficulty);
                    return;
                }
            }
            //宝獲得演出のエアー
            e.getClickedInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
        }
    }

    /**
     * プレイヤーがゲームエリア外に出た場合
     * @param e プレイヤーが移動をする
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!playerInGameArea(player) && hasChest) {
            player.sendMessage("エリア外にでました、ゲームを終了します");
            gameReset(player);
        }
    }

    /**
     * プレイヤーがゲームエリア内にいるか確認
     * @param player　プレイヤー
     * @return プレイヤーがゲームエリア内にいるか判定
     */
    public boolean playerInGameArea(Player player) {
        Location playerLocation = player.getLocation();
        int playerX = playerLocation.getBlockX();
        int playerZ = playerLocation.getBlockZ();

        return playerX >= (areaStartX -4) && playerX <= (areaEndX +4) &&
                playerZ >= (areaStartZ -7) && playerZ <= (areaEndZ + 4);

    }

    /**
     *レベル別にチェストにランダムに鉱石を入れていく
     * @param emptyChest　チェストリスト
     */

    private void putOreRandom(List<Block> emptyChest, String difficulty) {
        nowPlayerScore.setDifficulty(difficulty);


        List<Material> oreList = new ArrayList<>();
        oreList.add(Material.DIAMOND);
        oreList.add(Material.EMERALD);
        switch (difficulty) {
            case EASY -> {
                IntStream.range(0, 8).mapToObj(j -> Material.LAPIS_LAZULI).forEach(oreList::add);

                IntStream.range(0, 10).mapToObj(i -> Material.COAL).forEach(oreList::add);

                IntStream.range(0, 5).mapToObj(i -> Material.BONE).forEach(oreList::add);
            }
            case NORMAL -> {
                IntStream.range(0, 5).mapToObj(j -> Material.LAPIS_LAZULI).forEach(oreList::add);

                IntStream.range(0, 9).mapToObj(i -> Material.COAL).forEach(oreList::add);

                IntStream.range(0, 9).mapToObj(i -> Material.BONE).forEach(oreList::add);
            }
            case HARD -> {
                IntStream.range(0, 2).mapToObj(j -> Material.LAPIS_LAZULI).forEach(oreList::add);

                IntStream.range(0, 8).mapToObj(i -> Material.COAL).forEach(oreList::add);

                IntStream.range(0, 13).mapToObj(i -> Material.BONE).forEach(oreList::add);
            }
        }
        Collections.shuffle(oreList);

        oreList.stream().takeWhile(ore -> emptyChest.size() != 0).map(ItemStack::new).forEach(oreStack -> {
            Block treasureChest = emptyChest.remove(0);
            Inventory inventory = ((Chest) treasureChest.getState()).getInventory();
            inventory.addItem(oreStack);
        });
    }

    /**
     * ゲーム終了（ダイヤ有）まで、チェストの破壊を禁止する
     * @param e　チェストに攻撃
     */
    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if(hasDia && chestLocationList.contains(e.getBlock().getLocation())){
            e.setCancelled(true);
        }
    }
    /**
     * チェストが壊されたとき、ゲームを終了
     * @param e　チェストを壊したとき
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (chestIntLocation != null && block.getType() == Material.CHEST && hasChest && chestLocationList.contains(e.getBlock().getLocation())) {
            player.sendMessage("チェストが破壊されました、ゲームを終了します");
            gameReset(player);
        }
    }
    /**
     * 中身を取らずチェストをしめると、骨は減点・ダイヤは加点なしでゲーム終了
     * @param e　チェストをしめる
     */
    @EventHandler
    public void onChestClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest && hasChest) {
            Inventory chestInventory = chest.getInventory();
            Player player = (Player) e.getPlayer();
            // チェスト内のアイテムを調べる
            for (ItemStack item : chestInventory.getContents()) {
                if (chestIntLocation != null && item != null && item.getType() == Material.BONE) {
                    nowPlayerScore.setPoint(nowPlayerScore.getPoint() -20);
                    player.sendMessage("骨をスルー、ペナルティ-20点！現在のポイントは" + nowPlayerScore.getPoint() + "点！");
                }else if (chestIntLocation != null && item != null && item.getType() == Material.DIAMOND) {
                    player.sendMessage("ダイヤをそっと戻し、ダイヤ探しを中断した");
                    gameReset(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameReset(player);
    }
    /**
     *チェストやプレイヤースコアをリセット
     * @param player　プレイヤー
     */
    public void gameReset(Player player) {
        if (chestIntLocation != null) {
            chestIntLocation.stream().map(coords -> new Location(player.getWorld(), coords[0], coords[1], coords[2]))
                    .map(Location::getBlock).forEach(chestBlock -> chestBlock.setType(Material.AIR));
        }
        hasChest = false;
        nowPlayerScore = new PlayerScore();
    }

    @Override
    public boolean onExecutePlayerNPCCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }
}