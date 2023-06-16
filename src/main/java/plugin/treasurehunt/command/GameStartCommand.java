package plugin.treasurehunt.command;

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
import plugin.treasurehunt.ChestLocationDate;
import plugin.treasurehunt.PlayerScoreData;
import plugin.treasurehunt.data.ExecutingPlayer;
import plugin.treasurehunt.mapper.data.PlayerScore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

public class GameStartCommand extends BaseCommand implements Listener {

    public boolean hasChest;
    public boolean hasDia;
    public String difficulty;
    public static final String EASY ="easy";
    public static final String NORMAL ="normal";
    public static final String HARD ="hard";
    public static final String NONE ="none";
    public static final String LIST ="list";
    public static long startTime;
    private ChestLocationDate chestLocationDate;
    private PlayerScoreData playerScoreData = new PlayerScoreData();
    private ExecutingPlayer nowExecutingPlayer = new ExecutingPlayer();
    private List<int[]> chestIntLocation;
    private List<Location> chestLocationList;

    @Override
    public boolean onExecutePlayerCommand(Player player, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 1 && LIST.equals(args[0])){
            sendPlayerScoreList(player);
            return false;
        }
        difficulty = getDifficulty(player,args);
        if (difficulty.equals(NONE)) return false;
        hasFlag();
        gameplay(player, difficulty);
        return true;
    }

    /**
     * ゲームの実行と終了。周囲にチェストが現れ、鉱石がランダムで格納される。
     * ダイヤをゲットするとゲーム終了の処理をする。
     * @param player　プレイヤー
     * @param difficulty　ゲームの難易度
     */
    private void gameplay(Player player,String difficulty){
        //ゲーム終了
        if(!hasDia){
            long timeTaken = System.currentTimeMillis() - startTime;
            int secondsNumber = (int) (timeTaken / 1000);
            if(secondsNumber <= 10){
                nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +500);
            }else if(secondsNumber <= 20){
                nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +100);
            }
            player.sendTitle("ゲーム終了！" + secondsNumber + "秒"
                    ,difficulty + "モードでのあなたのスコアは" + nowExecutingPlayer.getPoint() + "点！");
            playerScoreData.insert(
                new PlayerScore(nowExecutingPlayer.getPoint()
                    ,secondsNumber
                    ,difficulty));
            scoreReset();
            return;
        }

        //ゲーム開始
        startTime = System.currentTimeMillis();
        this.chestLocationDate = new ChestLocationDate(player);
        List<Block> chestList = arrangementChest(player);
        putOreRandom(chestList, difficulty);
    }

    /**
     * プレイヤーの座標を取得し、チェストを等間隔で配置
     * @param player　プレイヤー
     * @return チェストの情報
     */
    public List<Block> arrangementChest(Player player) {

        List<Block> arrayChest = new ArrayList<>();
        chestIntLocation = new ArrayList<>();
        chestLocationList = new ArrayList<>();

        for (int x = ChestLocationDate.areaStartX; x <= ChestLocationDate.areaEndX; x += 2)
            for (int z = ChestLocationDate.areaStartZ; z <= ChestLocationDate.areaEndZ; z += 2) {
                Block block = player.getWorld().getBlockAt(x, ChestLocationDate.y, z);
                block.setType(Material.AIR);//二重置き防止
                block.setType(Material.CHEST);
                arrayChest.add(block);
                Location chestLocation = block.getLocation();
                chestIntLocation.add(new int[]{chestLocation.getBlockX(), chestLocation.getBlockY(), chestLocation.getBlockZ()});
            }
        for (int[] coords : chestIntLocation) {
            chestLocationList.add(new Location(player.getWorld(), coords[0], coords[1], coords[2]));
        }
        return arrayChest;
    }

    /**
     * コマンドの引数から難易度を取得
     * @param player　プレイヤー
     * @param args　コマンドの引数
     * @return　難易度
     */
    private String getDifficulty(Player player, @NotNull String[] args) {
        if(args.length == 1 && (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0]))){
            return args[0];
        }
        player.sendMessage("コマンドと半角スペースに続いて、難易度を入力してください(easy,normal,hard)");
        return NONE;
    }

    /**
     *チェストにランダムに鉱石を格納（レベル別）
     * @param emptyChest　ゲームスタートで配置したチェストのリスト
     */
    private void putOreRandom(List<Block> emptyChest, String difficulty) {
        nowExecutingPlayer.setDifficulty(difficulty);

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
     *ポイント加算（ゲーム中、ゲームエリア内のチェストインベントリをクリックした場合）
     * @param e チェストインベントリをクリック
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        //　生成したチェスト以外は対象外
        Player player = (Player) e.getWhoClicked();
        if (chestLocationList == null|| e.getCurrentItem() == null || !chestLocationList.contains(e.getInventory().getLocation())){
            return;}
        // インベントリ同士のアイテム移動を禁止
        if (Objects.requireNonNull(e.getClickedInventory()).getType() == InventoryType.PLAYER) {
            player.sendMessage("アイテムを入れるのは禁止です");
            e.setCancelled(true);
        }
        // クリックされたアイテムがチェスト内のものだったらポイント加算
        if (e.getClickedInventory().getType() == InventoryType.CHEST) {

            switch (e.getCurrentItem().getType()) {
                case DIAMOND -> {
                    chestReset(player);
                    player.sendMessage("ダイヤを見つけたよ！" );
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +100);
                    this.hasDia = false;
                    gameplay(player, difficulty);
                    return;
                }
                case EMERALD -> {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +100);
                    player.sendMessage("やった！エメラルドをゲット！現在のポイントは" + nowExecutingPlayer.getPoint() + "点！");
                }
                case LAPIS_LAZULI -> {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +50);
                    player.sendMessage("ラピスラズリをゲット！現在のポイントは" + nowExecutingPlayer.getPoint() + "点！");
                }
                case COAL -> {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() +20);
                    player.sendMessage("石炭をゲット！現在のポイントは" + nowExecutingPlayer.getPoint() + "点！");
                }
                case BONE -> {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() -5);
                    player.sendMessage("残念、骨だった・・・現在のポイントは" + nowExecutingPlayer.getPoint() + "点！");
                }
            }
            //チェストのアイテムをクリックするとエアーに（宝獲得演出）
            e.getClickedInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
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
        if (chestLocationDate == null) {
            return false;
        }
        return playerX >= (ChestLocationDate.areaStartX -4) && playerX <= (ChestLocationDate.areaEndX +4) &&
                playerZ >= (ChestLocationDate.areaStartZ -7) && playerZ <= (ChestLocationDate.areaEndZ + 4);

    }

    /**
     * プレイヤーがゲームエリア外に出た場合、ゲームを終了する
     * @param e プレイヤーが移動をする
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!playerInGameArea(player) && hasChest) {
            player.sendMessage("エリア外にでました、ゲームを終了します");
            chestReset(player);
            scoreReset();
        }
    }

    /**
     * ゲーム終了（ダイヤゲット）まで、チェストの破壊を禁止
     * @param e　チェストに攻撃
     */
    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if(hasDia && chestLocationList.contains(e.getBlock().getLocation())){
            e.setCancelled(true);
        }
    }

    /**
     * チェストが壊されたとき、ゲームを終了（Creative mode）
     * @param e　チェストを壊したとき
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (chestIntLocation != null && block.getType() == Material.CHEST && hasChest && chestLocationList.contains(e.getBlock().getLocation())) {
            player.sendMessage("チェストが破壊されました、ゲームを終了します");
            chestReset(player);
            scoreReset();
        }
    }

    /**
     * 中身を取らずチェストをしめると、骨は減点増（減点回避のペナルティ）・ダイヤは加点なしでゲーム終了（中身を先に確認の回避）
     * @param e　チェストをしめる
     */
    @EventHandler
    public void onChestClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest && hasChest) {
            Inventory chestInventory = chest.getInventory();
            Player player = (Player) e.getPlayer();
            for (ItemStack item : chestInventory.getContents()) {
                if (chestIntLocation != null && item != null && item.getType() == Material.BONE) {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() -20);
                    player.sendMessage("骨をスルー、ペナルティ-20点！現在のポイントは" + nowExecutingPlayer.getPoint() + "点！");
                }else if (chestIntLocation != null && item != null && item.getType() == Material.DIAMOND) {
                    player.sendMessage("ダイヤをそっと戻し、ダイヤ探しを中断した");
                    chestReset(player);
                }
            }
        }
    }

    /**
     * ゲーム中にログアウトするとチェストをリセット（チェスト増殖対策）
     * @param event　プレイヤーがログアウト
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        chestReset(player);
        scoreReset();
    }

    /**
     * 出現させたチェストを削除する
     * @param player　プレイヤー
     */
    public void chestReset(Player player){
        if (chestIntLocation != null) {
            chestIntLocation.stream().map(coords -> new Location(player.getWorld(), coords[0], coords[1], coords[2]))
                    .map(Location::getBlock).forEach(chestBlock -> chestBlock.setType(Material.AIR));
            this.hasChest = false;
        }
    }

    /**
     * スコアのリセット
     */
    public void scoreReset() {
        nowExecutingPlayer = new ExecutingPlayer();
    }

    /**
     * ゲーム開始（チェストの出現）と、ゲーム終了（チェストからダイヤがなくなる）のフラグ用
     */
    private void hasFlag() {
        this.hasChest = true;
        this.hasDia = true;
    }

    /**
     *登録されているスコアリストをメッセージに送る
     * @param player　プレイヤー
     */
    private void sendPlayerScoreList(Player player) {
        List<PlayerScore> playerScoreList = playerScoreData.selectList();
        for(PlayerScore playerScore : playerScoreList){
            player.sendMessage("｜"
                    + playerScore.getId() + "｜"
                    + playerScore.getDate() + "｜"
                    + playerScore.getScore() + "｜"
                    + playerScore.getTime() + "｜"
                    + playerScore.getDifficulty() + "｜");
        }
    }

    @Override
    public boolean onExecutePlayerNPCCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }
}