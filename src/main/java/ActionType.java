package main.java;

/**
 * Возможные действия волшебника.
 * <p>
 * Волшебник не может совершать новые действия, если он ещё не восстановился после своего предыдущего действия (значение
 * {@code wizard.remainingActionCooldownTicks} больше {@code 0}).
 * <p>
 * Волшебник не может использовать действие, если оно ещё не восстановилось после его предыдущего применения (значение
 * {@code remainingCooldownTicksByAction[actionType.ordinal()]} больше {@code 0}).
 */
public enum ActionType {
    /**
     * Ничего не делать.
     */
    NONE,

    /**
     * Ударить посохом.
     * <p>
     * Атака поражает все живые объекты в секторе от {@code -game.staffSector / 2.0} до {@code game.staffSector / 2.0}.
     * Расстояние от центра волшебника до центра цели не должно превышать значение
     * {@code game.staffRange + livingUnit.radius}.
     */
    STAFF,

    /**
     * Создать магическую ракету.
     * <p>
     * Магическая ракета является базовым заклинанием любого волшебника. Наносит урон при прямом попадании.
     * <p>
     * При создании магической ракеты её центр совпадает с центром волшебника, направление определяется как
     * {@code wizard.angle + move.castAngle}, а абсолютное значение скорости равно {@code game.magicMissileSpeed}.
     * Столкновения магического снаряда и создавшего его волшебника игнорируются.
     * <p>
     * Требует {@code game.magicMissileManacost} единиц магической энергии.
     */
    MAGIC_MISSILE,

    /**
     * Создать ледяную стрелу.
     * <p>
     * Ледяная стрела наносит урон при прямом попадании, а также замораживает цель.
     * <p>
     * При создании ледяной стрелы её центр совпадает с центром волшебника, направление определяется как
     * {@code wizard.angle + move.castAngle}, а абсолютное значение скорости равно {@code game.frostBoltSpeed}.
     * Столкновения магического снаряда и создавшего его волшебника игнорируются.
     * <p>
     * Требует {@code game.frostBoltManacost} единиц магической энергии и изучения умения {@code FROST_BOLT}.
     */
    FROST_BOLT,

    /**
     * Создать огненный шар.
     * <p>
     * Огненный шар взрывается при достижении максимальной дальности полёта или при столкновении с живым объектом.
     * Наносит урон всем близлежащим живым объектам, а также поджигает их.
     * <p>
     * При создании огненного шара его центр совпадает с центром волшебника, направление определяется как
     * {@code wizard.angle + move.castAngle}, а абсолютное значение скорости равно {@code game.fireballSpeed}.
     * Столкновения магического снаряда и создавшего его волшебника игнорируются.
     * <p>
     * Требует {@code game.fireballManacost} единиц магической энергии и изучения умения {@code FIREBALL}.
     */
    FIREBALL,

    /**
     * Временно ускорить волшебника с идентификатором {@code move.statusTargetId} или самого себя, если такой волшебник
     * не найден.
     * <p>
     * Требует {@code game.hasteManacost} единиц магической энергии и изучения умения {@code HASTE}.
     */
    HASTE,

    /**
     * На время создать магический щит вокруг волшебника с идентификатором {@code move.statusTargetId} или самого себя,
     * если такой волшебник не найден.
     * <p>
     * Требует {@code game.shieldManacost} единиц магической энергии и изучения умения {@code SHIELD}.
     */
    SHIELD
}
