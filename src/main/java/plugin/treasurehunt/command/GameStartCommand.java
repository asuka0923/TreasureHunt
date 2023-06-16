package plugin.treasurehunt.command;

import java.util.Arrays;

import org.bukkit.ChatColor;
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
    public static long startTime;
    public String difficulty;
    public static final String EASY ="easy";
    public static final String NORMAL ="normal";
    public static final String HARD ="hard";
    public static final String NONE ="none";
    public static final String LIST ="list";
    public static final String RULE ="rule";
    private List<Location> chestLocationList;
    private ChestLocationDate chestLocationDate;
    private List<Material> oreList;
    private PlayerScoreData playerScoreData = new PlayerScoreData();
    private ExecutingPlayer nowExecutingPlayer = new ExecutingPlayer();

    @Override
    public boolean onExecutePlayerCommand(Player player, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 1 && LIST.equals(args[0])){
            sendPlayerScoreList(player);
            return false;
        }else if(args.length == 1 && RULE.equals(args[0])){
            gameRule(player);
            return false;
        }
        difficulty = getDifficulty(player,args);
        if (difficulty.equals(NONE)) return false;
        hasFlag();
        startCountdown(player);
        gameplay(player, difficulty);
        return true;
    }

    /**
     * ゲームスタート時のカウントダウン
     * @param player　プレイヤー
     */
    private void startCountdown(Player player) {
        int countdownTime = 10;
        for (int i = countdownTime; i > 0; i--) {
            player.sendTitle(String.valueOf(i),"ゲーム開始位置についてください");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        player.sendTitle("GO!!","ゲームスタート！");
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
                    ,difficulty + "モードでのあなたのスコアは"
                    + nowExecutingPlayer.getPoint() + "点！");
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
        chestLocationList = new ArrayList<>();

        for (int x = ChestLocationDate.areaStartX; x <= ChestLocationDate.areaEndX; x += 2)
            for (int z = ChestLocationDate.areaStartZ; z <= ChestLocationDate.areaEndZ; z += 2) {
                Block block = player.getWorld().getBlockAt(x, ChestLocationDate.y, z);
                block.setType(Material.AIR);//二重置き防止
                block.setType(Material.CHEST);
                arrayChest.add(block);
                Location chestLocation = block.getLocation();
                chestLocationList.add(chestLocation);
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
        oreList = new ArrayList<>();
        oreList.addAll(Arrays.asList(Material.DIAMOND, Material.EMERALD));
        switch (difficulty) {
            case EASY -> {
                addMaterialsToOreList(8, Material.LAPIS_LAZULI);
                addMaterialsToOreList(10, Material.COAL);
                addMaterialsToOreList(5, Material.BONE);
            }
            case NORMAL -> {
                addMaterialsToOreList(5, Material.LAPIS_LAZULI);
                addMaterialsToOreList(9, Material.COAL);
                addMaterialsToOreList(9, Material.BONE);
            }
            case HARD -> {
                addMaterialsToOreList(2, Material.LAPIS_LAZULI);
                addMaterialsToOreList(8, Material.COAL);
                addMaterialsToOreList(13, Material.BONE);
            }
        }
        Collections.shuffle(oreList);

        oreList.stream()
            .takeWhile(ore -> emptyChest.size() != 0)
            .map(ItemStack::new)
            .forEach(oreStack -> {Block treasureChest = emptyChest.remove(0);
            Inventory inventory = ((Chest) treasureChest.getState()).getInventory();
            inventory.addItem(oreStack);
        });
    }

    /**
     *鉱石別にポイント加算（ゲーム中、ゲームエリア内のチェストインベントリをクリックした場合）
     * @param e チェストインベントリをクリック
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        //　生成したチェスト以外は対象外
        Player player = (Player) e.getWhoClicked();
        if (chestLocationList == null||
            e.getCurrentItem() == null ||
            !chestLocationList.contains(e.getInventory().getLocation())){
            return;
        }
        // チェストにアイテムを入れるのを禁止
        if (Objects.requireNonNull(
            e.getClickedInventory()).getType() == InventoryType.PLAYER) {
            player.sendMessage("アイテムを入れるのは禁止です");
            e.setCancelled(true);
        }
        // クリックされたアイテムがチェスト内のものだったらポイント加算
        if (e.getClickedInventory().getType() == InventoryType.CHEST) {
            Material itemType = e.getCurrentItem().getType();
            int pointToAdd = 0;
            String message = "";
            switch (itemType) {
                case DIAMOND:
                    chestReset(player);
                    message = "ダイヤを見つけたよ！おつかれさま！";
                    pointToAdd = 100;
                    this.hasDia = false;
                    gameplay(player, difficulty);
                    break;
                case EMERALD:
                    message = "やった！エメラルドをゲット！現在のポイントは"
                        + nowExecutingPlayer.getPoint() + "点！";
                    pointToAdd = 100;
                    break;
                case LAPIS_LAZULI:
                    message = "ラピスラズリをゲット！現在のポイントは"
                        + nowExecutingPlayer.getPoint() + "点！";
                    pointToAdd = 50;
                    break;
                case COAL:
                    message = "石炭をゲット！現在のポイントは"
                        + nowExecutingPlayer.getPoint() + "点！";
                    pointToAdd = 20;
                    break;
                case BONE:
                    message = "残念、骨だった・・・現在のポイントは"
                        + nowExecutingPlayer.getPoint() + "点！";
                    pointToAdd = -5;
                    break;
            }
            nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() + pointToAdd);
            player.sendMessage(message);
            //チェストのアイテムはクリックでエアーに
            e.getClickedInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
        }
    }

    /**
     *それぞれの鉱石をレベル別で決められた数入れていく
     * @param count　レベル別に決められた数
     * @param material　鉱石の種類
     */
    private void addMaterialsToOreList(int count, Material material) {
        IntStream.range(0, count).mapToObj(i -> material).forEach(oreList::add);
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
        return playerX >= (ChestLocationDate.areaStartX -4) &&
                playerX <= (ChestLocationDate.areaEndX +4) &&
                playerZ >= (ChestLocationDate.areaStartZ -7) &&
                playerZ <= (ChestLocationDate.areaEndZ + 4);

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
     * ゲーム終了（ダイヤゲット）まで、チェストの破壊を禁止（Creative以外）
     * @param e　チェストに攻撃
     */
    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if(hasDia &&
            chestLocationList.contains(e.getBlock().getLocation())){
            e.setCancelled(true);
        }
    }

    /**
     * チェストが壊されたとき、ゲームを終了（Creativeモード）
     * @param e　チェストを壊したとき
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (chestLocationList != null &&
            block.getType() == Material.CHEST &&
            hasChest &&
            chestLocationList.contains(e.getBlock().getLocation())) {
            player.sendMessage("チェストが破壊されました、ゲームを終了します");
            chestReset(player);
            scoreReset();
        }
    }

    /**
     * 中身を取らずチェストをしめると、骨は減点増（減点回避のペナルティ）・ダイヤは加点なしでゲーム終了（中身の確認のみを回避）
     * @param e　チェストをしめる
     */
    @EventHandler
    public void onChestClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest && hasChest) {
            Inventory chestInventory = chest.getInventory();
            Player player = (Player) e.getPlayer();

            for (ItemStack item : chestInventory.getContents()) {
                if (chestLocationList != null &&
                    item != null &&
                    item.getType() == Material.BONE) {
                    nowExecutingPlayer.setPoint(nowExecutingPlayer.getPoint() -20);
                    player.sendMessage("骨をスルー、ペナルティ-20点！現在のポイントは"
                        + nowExecutingPlayer.getPoint() + "点！");

                }else if (chestLocationList != null &&
                    item != null &&
                    item.getType() == Material.DIAMOND) {
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
        if (chestLocationList != null) {
            chestLocationList.stream()
                .map(Location::getBlock)
                .forEach(chestBlock -> chestBlock.setType(Material.AIR));
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

    /**
     * ゲームのルール説明
     * @param player プレイヤー
     */
    private static void gameRule(Player player) {
        player.sendMessage("このゲームはコマンド入力でチェストが配置されます" +
                "\nチェストの中の鉱石別でポイントが付与されます。" +
                "\nレベルは、easy、normal、hardがあります。" +
                "\n付与ポイントは、ダイヤ100点・エメラルド100点、" +
                "\nラピスラズリ50点・石炭20点で、骨は-5点です。" +
                "\nボーナスは、10秒以内クリアで500点、20秒以内クリアで100点です。" +
                "\nダイヤを見つけるとゲーム終了になります。");
        player.sendMessage(ChatColor.GREEN + "・何もない場所でプレイしてください。" +
                "\n・チェストは、現在地から南方向に配置されます");
    }

    /**
     * ゲーム開始（チェストの出現）と、ゲーム終了（チェストからダイヤがなくなる）のフラグ用
     */
    private void hasFlag() {
        this.hasChest = true;
        this.hasDia = true;
    }

    @Override
    public boolean onExecutePlayerNPCCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }
}