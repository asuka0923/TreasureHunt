package plugin.treasurehunt.data;

import lombok.Getter;
import lombok.Setter;

/**
 * ゲームのスコア情報を扱う
 * 日時・難易度・スコア
 */
@Getter
@Setter
public class ExecutingPlayer {
  private int point;
  private int time;
  private String difficulty;
}
