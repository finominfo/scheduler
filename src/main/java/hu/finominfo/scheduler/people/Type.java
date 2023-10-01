package hu.finominfo.scheduler.people;

/**
 * Created by kks on 2017.12.25..
 */
public enum Type {
  FO("IMS1"),
  BO("IMS2"),
  FO_AND_BO("IMS");

  private final String text;

  private Type(String str) {
    text = str;
  }

  public boolean goodWith(Type type) {
    return !this.equals(type) || this.equals(FO_AND_BO);
  }

  public static boolean isFirstFo(Type t1, Type t2) {
    return t1.equals(FO) || t2.equals(BO);
  }

  public String toCell() {
    return text;
  }
}
