package main.java;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class MyStrategy implements Strategy {
    private static final double WAYPOINT_RADIUS = 300.0D;
    private static final double LOW_HP_FACTOR = 0.33D;
    private static double HOLD_DISTANCE_FACTOR_CONST = 300D;
    private static double HOLD_DISTANCE_FACTOR = 0.5D;
    private static double CHECK_OBSTACACLES_RADIUS = 400;
    private static double MOVE_AROUND_OBSTACACLE_RADIUS = 15;
    private static double mapPad = 250;
    
    
    Queue<SkillType> SKILLS_TO_LEARN = new LinkedList<SkillType>();
    Map<SkillType, List<SkillType>> SKILLS_LISTING = new HashMap<SkillType, List<SkillType>>();
    List<SkillType> SKILLS_QUEUE = new ArrayList<SkillType>();
    
    Forest FOREST;
    PowerMap POWER_MAP;
    
    
    List<Point2D> bonuses = new ArrayList<Point2D>();
    private Map<Point2D, Boolean> bonusExist = new HashMap<Point2D, Boolean>();
    boolean moveToBonuses = false;
    double CAN_MOVE_TO_BONUS_TICK_COUNT = 400;
    
    
    List<Projectile> projectiles = new ArrayList<Projectile>();
    Map<Projectile, Point2D> projectilesStart = new HashMap<Projectile, Point2D>();
    
    
    boolean goToBonus;
    boolean goToLane;
    boolean isRetreat;
    boolean isDefeat;
    boolean isDefeatLane;
    
    
    boolean isRetreatToForest; 
    boolean isRetreatInForest;
    boolean isWayDirect;		// flag to create way direct to point
    

    private Random random;
    
    Point2D myLastPosition;									// used for check death
    
    Point2D stuckPoint;
    int stuckTick = 0;

    
    // ==================== WAYPOINTS VARS ====================
    
    final Map<LaneType, Waypoint> targetWaypointByLane = new EnumMap<>(LaneType.class);
    LaneType lane;
    
    Set<Waypoint> mapPoints = new HashSet<Waypoint>();						// grid points on map
    Set<Waypoint> waypoints = new HashSet<Waypoint>();						// for look nearest path
    List<Waypoint> waypointsToMove = new ArrayList<Waypoint>();				// track to move
    List<Waypoint> waypointsToBonus = new ArrayList<Waypoint>();			// track to bonus
    Waypoint HOME_POINT;													// home base
    Waypoint ENEMY_HOME_POINT;												// enemy base
    
    
    // ==================== GAME VARS ====================
    
    private Wizard SELF;
    private World WORLD;
    private Game GAME;
    private Move MOVE;
    

    // ==================== UNITS VARS ====================
    
    List<CircularUnit> circularUnits;								// All CircularUnits without SELF
    List<CircularUnit> circularUnitsInCheckObstacleRadius;			// All CircularUnits without SELF
    List<LivingUnit> units;											// All LivingUnits
    List<CircularUnit> treeUnits;									// trees
    List<LivingUnit> enemyUnits;									// Enemies
    List<LivingUnit> friendUnits;									// Friends
    List<LivingUnit> friendUnitsExcludeMe;							// Friends without me
    List<LivingUnit> enemiesInCastRange;							// Enemies in my cast range
    List<LivingUnit> enemiesInStaffRange;							// Enemies in my staff range
    List<LivingUnit> threatenedMeUnits;								// Enemies who can damage me
    List<Wizard> enemyWizards = new ArrayList<Wizard>();
    List<Wizard> friendWizards = new ArrayList<Wizard>();
    List<LivingUnit> enemiesWithAggressiveMinions;
    List<LivingUnit> neutralsMinions = new ArrayList<LivingUnit>();
    List<LivingUnit> aggressiveMinions = new ArrayList<LivingUnit>();
    List<Building> enemiesBuildingsList = new ArrayList<Building>();
    Map<LaneType, Integer> enemyBuildingsByLane = new EnumMap<>(LaneType.class);
    Map<LaneType, List<Wizard>> enemyWizardsByLane = new HashMap<LaneType, List<Wizard>>();
    Map<LaneType, List<Wizard>> friendWizardsByLane = new HashMap<LaneType, List<Wizard>>();
    Map<Building, Integer> enemyBuildingsCD = new HashMap<Building, Integer>();
    
    
    CircularUnit enemyTarget;						// Enemy I attack
    ActionType actionType;
    CircularUnit obstacleTarget;					// obstacle I attack
    Point2D targetTurn;								// point I look on
    Point2D targetPoint;							// point I move to
    Point2D targetBonus;
    Point2D pointToAvoidMagicAttack;
    
    
    boolean isFrostBoltEnable = false;
    boolean isFireballEnable = false;
    boolean isHasteEnable = false;
    boolean isShieldEnable = false;
    
    boolean makeWaySafe = true;
    
    
    boolean isTurnRetreat = false;
    
    
    boolean moveBackward = false;
    Projectile avoidProjectile;
    
    ROLE myRole = null;
    enum ROLE {RUSH, DEFEAT, HASTE};
    Map<ROLE, Queue<SkillType>> roleSkills = new HashMap<ROLE, Queue<SkillType>>();
    
    
    Map<Wizard, Double> wizardForwardSpeed = new HashMap<Wizard, Double>();
    Map<Wizard, Double> wizardBackwardSpeed = new HashMap<Wizard, Double>();
    Map<Wizard, Double> wizardRotateSpeed = new HashMap<Wizard, Double>();
    Map<Wizard, Double> wizardStrafeSpeed = new HashMap<Wizard, Double>();
    Map<Wizard, Double> wizardCastRange = new HashMap<Wizard, Double>();
    Map<Wizard, Double> wizardMagicDamage = new HashMap<Wizard, Double>();
    Map<Wizard, Double> wizardStaffDamage = new HashMap<Wizard, Double>();
    
    
    int enemyWizardsCnt = 0; 
    
    
    
    boolean isCommandLaneRush = false;
    
    Map<Wizard, LaneType> wizardLane = new HashMap<Wizard, LaneType>();
    List<Message> MESSAGES = new ArrayList<Message>(10);
    Map<Integer, LaneType> messageLane = new HashMap<Integer, LaneType>();
    Map<Integer, SkillType> messageSkill = new HashMap<Integer, SkillType>();
    Map<Integer, byte[]> messageRaw = new HashMap<Integer, byte[]>();
    LaneType commandLane = null;
    
    
    
    
    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {   	
    	
    	// ================ init game =================

        this.SELF = self;
        this.WORLD = world;
        this.GAME = game;
        this.MOVE = move;    

        if (random == null) {
        	initializeStrategy(self, game);
        }
if (WORLD.getTickIndex() >= 5070) {
	int i = 0;
	i++;
}
        initializeTick(self, world, game, move);
        
        // ============== command play ===========
        // getMessage
        if (GAME.isRawMessagesEnabled()) {
	        List<Message> messages = Arrays.asList(SELF.getMessages());
	        if (messages.size() != 0) {
	        	lane = commandLane = messages.get(0).getLane();
	        }
        }


        // ============== after death ==================
        
        if (new Point2D(SELF).getDistanceTo(myLastPosition) > 10) {
        	restart();
        }
        
        myLastPosition = new Point2D(SELF);

        // ================= Defeat base =================

        estimateDefeat();


        // ====== check for available bonus move =======

    	if (canMoveToBonus()) {
    		goToBonus = true;
    	} else {
    		goToBonus = false;
    	}
    	
        
  	   // ========= find target i will shoot =========== 
  	   
  	   chooseEnemyTarget();
    	
   
        // ================= On low HP ====================

        estimateLifeAction();
        
        
        // ============= Choose lane ======================
        
        if (!GAME.isRawMessagesEnabled()) {
        	chooseLane();
        }
        if (GAME.isRawMessagesEnabled() && commandLane == null) {
        	chooseLane();
        }
        if (POWER_MAP.getRectLane(POWER_MAP.getMyRect()) != lane)
        	goToLane = true;
        else 
        	goToLane = false;
        /*lane = LaneType.TOP;
        /*goToBonus = false;*/

        
	
	   // =============== hold distance ==================

        holdDistance();

    	
    	if (isRetreat) {
        	retreat();
        	
        	if (POWER_MAP.getPointLane(new Point2D(SELF)) == null)
        		goToLane = false;
        } 
    	
    	else if (isDefeat) {
    		targetPoint = HOME_POINT.point.clone();
    		
        	if (goToBonus && targetBonus.getDistanceTo(SELF) <= 400)
        		targetPoint = targetBonus;
        }
    	
    	else if (isDefeatLane) {
    		// ========= command play ========
    		if (GAME.isRawMessagesEnabled()) {
	    		if (friendWizardsByLane.get(lane).size() > 3) {
	    			LaneType lane1, lane2;
	    			if (lane == LaneType.TOP) {
	    				lane1 = LaneType.MIDDLE;
	    				lane2 = LaneType.BOTTOM;
	    			} else if (lane == LaneType.MIDDLE) {
	    				lane1 = LaneType.TOP;
	    				lane2 = LaneType.BOTTOM;
	    			} else {
	    				lane1 = LaneType.TOP;
	    				lane2 = LaneType.MIDDLE;
	    			}
	    			
	    			boolean toLane1 = true, toLane2 = true;
	    			Point2D p1 = POWER_MAP.getRectsByLane(lane1).get(0).getCenter(),
	    					p2 = POWER_MAP.getRectsByLane(lane2).get(0).getCenter();
	    			
	    			for (Wizard w: friendWizards) {
	    				if (p1.getDistanceTo(w) < p1.getDistanceTo(SELF))
	    					toLane1 = false;
	    				if (p2.getDistanceTo(w) < p2.getDistanceTo(SELF))
	    					toLane2 = false;
	    			}
	    			
	    			if (toLane1) 
	    				lane = lane1;
	    			if (toLane2)
	    				lane = lane2;
	    		}
	    		
	    		
	    		int top = 0, mid = 0, bottom = 0;
	    		Point2D pointTop = POWER_MAP.getNextByLane(LaneType.TOP).getCenter();
	    		Point2D pointMid = POWER_MAP.getNextByLane(LaneType.MIDDLE).getCenter();
	    		Point2D pointBottom = POWER_MAP.getNextByLane(LaneType.BOTTOM).getCenter();
	    		for (Wizard w: friendWizards) {
	    			if (pointTop.getDistanceTo(w) < pointTop.getDistanceTo(SELF))
	    				top++;
	    			if (pointMid.getDistanceTo(w) < pointMid.getDistanceTo(SELF))
	    				mid++;
	    			if (pointBottom.getDistanceTo(w) < pointBottom.getDistanceTo(SELF))
	    				bottom++;
	    		}
	    		if (enemyWizardsByLane.get(LaneType.MIDDLE).size() > mid)
	    			lane = LaneType.MIDDLE;
	    		else if (enemyWizardsByLane.get(LaneType.TOP).size() > top)
	    			lane = LaneType.TOP;
    		}
    			
    		
    		targetPoint = getNextPointOnLane(lane);
        	
    		doMoveStaff();
    	}
    	
    	else {
    		if (!goToLane) {
    			Point2D p = getPointToAvoidMinionAppearLock();
    			if (p != null)
    				targetPoint = p;
    		}
    		
        	if (targetPoint == null) {
        		
        		targetPoint = getNextPointOnLane(lane);
            	
        		doMoveStaff();
            	
	        	if (targetPoint == null) {
	            	targetPoint = ENEMY_HOME_POINT.point.clone();
	        	}
	        	
        	}
        	
        	if (goToBonus)
        		targetPoint = targetBonus;
        	
        	
    		// command play
    		if (GAME.isRawMessagesEnabled() && !goToBonus) {
    			/*List<Message> messages = Arrays.asList(SELF.getMessages());
    	        if (messages.size() != 0) {
    	        	byte[] raw = messages.get(0).getRawMessage();
    	        	if (raw.length > 0 && raw[0] == (byte)0)
    	        		targetPoint = POWER_MAP.middleRects.get(4).getCenter();
    	        }*/
    			if (enemyWizardsCnt == 5 
    					&& enemyWizardsByLane.get(LaneType.BOTTOM).size() == 0
    					&& enemyWizardsByLane.get(LaneType.TOP).size() == 0
    					&& POWER_MAP.middleRects.indexOf(POWER_MAP.getNextMiddle()) <= 5) {
	    			boolean existBuilding = false;
	    			for (Building b: WORLD.getBuildings()) {
	    				if (new Point2D(902, 2768).getDistanceTo(b) < 10) {
	    					existBuilding = true;
	    					break;
	    				}
	    			}
	    			double life = 0;
	    			if (WORLD.getTickIndex() >= 604) {
	    				int i = 0;
	    				i++;
	    			}
	    			for (Wizard w: enemyWizards)
	    				life += w.getLife();
	    			
	    			if ((life <= GAME.getWizardBaseLife() * 4 
	    					&& existBuilding 
	    					&& enemyBuildingsByLane.get(lane) > 0
	    					&& enemyWizardsByLane.get(LaneType.MIDDLE).size() == 5)
	    					
	    				|| (POWER_MAP.middleRects.indexOf(POWER_MAP.getNextMiddle()) <= 3
	    						&& SELF.getLevel() >= 5
		    					&& enemyBuildingsByLane.get(lane) > 0
		    					/*&& enemyWizardsByLane.get(LaneType.MIDDLE).size() >= 4*/)
	    			) {
	    				lane = LaneType.MIDDLE;
	    				if (POWER_MAP.getRectLane(POWER_MAP.getMyRect()) == null
	    						&& new Point2D(2000,2000).getDistanceTo(SELF) > 600)
	    					targetPoint = new Point2D(2000, 2000);
	    			}
    			}
    		}
    	}

    	checkStuck();
    	
    	avoidMagicAttack();

    	isWayDirect = true;
    	go();


    	if (enemyTarget == null)
    		enemyTarget = obstacleTarget;

    	
    	/*if (obstacleTarget == null)
    		obstacleTarget = findForestCornerObstacles();*/
    	chooseEnemyTarget();
        attack();

        setTurnAfterMove(targetTurn);

        learnSkill();
        
        haste();
        
        
        // ========= command play ========
        //if (GAME.isRawMessagesEnabled()) {
        	if (SELF.isMaster()) {
        		//setMessages();
        	}
        //}
        

        /*System.out.println(WORLD.getTickIndex());
        if (WORLD.getTickIndex() >= 6712 && WORLD.getTickIndex() <= 7930) {
	    	int i = 0;
    	    System.out.println(isRetreat + " " + isDefeat + " " + isDefeatLane + " ' " + new Point2D(SELF) + " " + targetPoint);
	    }*/
        	
        
    }
    
    
    Map<Integer, LaneType> getTeamLanes() {
    	Map<Integer, LaneType> res = new HashMap<Integer, LaneType>();
    	
    	double friendTicks = 0, enemyTicks = 0;
    	Map<Integer, LaneType> list = new HashMap<Integer, LaneType>();
    	List<LaneType> laneTypes = Arrays.asList(new LaneType[] {LaneType.TOP, LaneType.MIDDLE, LaneType.BOTTOM});
    	

    	for (int i = 0; i < 3*3*3*3*3; i++) {
    		int p = i;
        	int start = 1;
        	if (SELF.getId() > 5)
        		start = 6;
        	System.out.println(p);
    		for (int j = 0; j < 5; j++, start++) {
    			int k = p % (int)(Math.pow(3, j));
    			list.put(start, laneTypes.get(k));
    			p -= k * (int)Math.pow(3, j);
    			System.out.println(p);
    		}
    		
    		res = list;
    		System.out.println(res);
    	}
    	
    	return res;
    }
    
    
    void setMessages() {
    	if (enemyWizardsCnt == 5) {
    		/*int enemyCntInCenter = 0;
    		for (Wizard w: enemyWizards) {
    			if (new Point2D(1930, 2400).getDistanceTo(w) < 550)
    				enemyCntInCenter++;
    		}
    		if (enemyCntInCenter >= 4) {
	    		for (Wizard w: friendWizards) {
	    			if (w == SELF)
	    				continue;
	    			
	    			messageLane.put((int)w.getId(), LaneType.MIDDLE);
	    			
	    			if (POWER_MAP.getPointLane(new Point2D(w)) != LaneType.MIDDLE) {
	    				messageRaw.put((int)w.getId(), new byte[] {(byte)0} );
	    			}
	    		}
    		}*/
    		
    	} else {
    		for (Wizard w: friendWizards) {
    			if (w == SELF)
    				continue;
    			if (w.getId() % 5 == 0 || w.getId() % 5 == 4)
    				messageLane.put((int)w.getId(), LaneType.BOTTOM);
    			else
    				messageLane.put((int)w.getId(), LaneType.MIDDLE);
    		}
    	}
    	
    	
    	List<Message> list = new ArrayList<Message>();
    	int start = 1;
    	if (SELF.getId() > 5)
    		start = 6;
    	
    	for (int i = 0; i < 5; i++, start++) {
    		if (start == (int)SELF.getId())
    			continue;
    		
    		list.add(new Message(messageLane.get(start), null, new byte[0]));
    	}
    	

    	Message[] res = list.toArray(new Message[list.size()]);
    	MOVE.setMessages(res);
    }
    
    
    boolean estimateRush() {
    	if (enemyWizardsCnt < 5)
    		return false;
    	
    	
    	double friendDPM = 0, enemyDPM = 0,
    			friendLife = 0, enemyLife = 0;
    	
    	for (Wizard f: friendWizards) {
    		if (new Point2D(SELF).getDistanceTo(f) < 500) {
    			friendDPM += getUnitDPM(f);
    			friendLife += getUnitLife(f);
    		}
    	}
    	
    	
    	List<LivingUnit> list = new ArrayList<LivingUnit>();
    	List<Wizard> wizardsByLane = enemyWizardsByLane.get(lane);
    	list.addAll(wizardsByLane);
    	outer:
    	for (LivingUnit u: enemyUnits) {
    		for (Wizard w: wizardsByLane) {
    			if (w.getId() == u.getId())
    				continue outer;
    		}
			list.add(u);
    	}
    	
    	for (LivingUnit e: list) {
    		if (e instanceof Minion)
    			continue;
    		
    		double dist = 600;
    		if (e instanceof Building)
    			dist = ((Building)e).getAttackRange() + 50;
    		if (new Point2D(SELF).getDistanceTo(e) < dist) {
    			enemyDPM += getUnitDPM(e);
    			enemyLife += getUnitLife(e);
    		}
    	}
    	
    	
    	if (enemyDPM == 0 || enemyLife / friendDPM * 1.2 < friendLife / enemyDPM)
    		return true;
    	
    	
    	return false;
    }
    
    
    void haste() {
    	if (MOVE.getAction() != null 
    			|| !isHasteEnable 
    			|| SELF.getMana() < GAME.getHasteManacost()
    			|| SELF.getRemainingActionCooldownTicks() > 0
    			|| SELF.getRemainingCooldownTicksByAction()[5] > 0)
    		return;
		
		if (GAME.isRawMessagesEnabled()) {
			Wizard target = null;
			for (Wizard w: friendWizards) {
	    		if (w.getDistanceTo(SELF) < wizardCastRange.get(SELF) && SELF != w && !isWizardHastened(w)) {
	    			if (target == null || Math.abs(SELF.getAngleTo(target)) > Math.abs(SELF.getAngleTo(w)))
	    				target = w;
	    		}
			}
			
			if (target == null && !isWizardHastened(SELF)) {
				MOVE.setAction(ActionType.HASTE);
				MOVE.setStatusTargetId(-1);
				return;
			}
			
			if (target != null) {
				Point2D point = new Point2D(SELF);
				Point2D center = new Point2D(target);
				double lookAngle = SELF.getAngle();
				double sectorAngle = GAME.getStaffSector();
				
				Line l1 = new Line(point, normalizeAngle(lookAngle + sectorAngle/2));
				Line l2 = new Line(point, normalizeAngle(lookAngle - sectorAngle/2));
				
				if ( l1.getRelativePointPosition(center) + l1.getRelativePointPosition(l2.end) != 0
						&& l2.getRelativePointPosition(center) + l2.getRelativePointPosition(l1.end) != 0
						&& normalizeAngle(StrictMath.abs(SELF.getAngle() - new Line(new Point2D(SELF), new Point2D(target)).angle)) < Math.PI / 2) {
					MOVE.setAction(ActionType.HASTE);
					MOVE.setStatusTargetId(target.getId());
					return;
				} else {
					MOVE.setTurn(SELF.getAngleTo(target));
				}
			}
				
			
		} else {  	
			if (!Arrays.asList(SELF.getStatuses()).contains(StatusType.HASTENED)) {
				MOVE.setAction(ActionType.HASTE);
				MOVE.setStatusTargetId(-1);
				return;
			}
		}
    }
    
    
    CircularUnit findForestCornerObstacles() {
    	for (LivingUnit u: enemyUnits)
    		if (u.getDistanceTo(SELF) < 700)
    			return null;
    	
    	
    	CircularUnit res = null;
    	
    	Point2D nearestCorner = new Point2D(0,0);
    	for (Triangle t: FOREST.parts) {
    		if (t.p1.getDistanceTo(SELF) < nearestCorner.getDistanceTo(SELF))
    			nearestCorner = t.p1;
    		if (t.p2.getDistanceTo(SELF) < nearestCorner.getDistanceTo(SELF))
    			nearestCorner = t.p2;
    		if (t.p3.getDistanceTo(SELF) < nearestCorner.getDistanceTo(SELF))
    			nearestCorner = t.p3;
    	}
    	
    	final Point2D p = nearestCorner;
    	
    	if (nearestCorner.getDistanceTo(new Point2D(SELF)) < 600) {
    		res = (CircularUnit)Arrays.asList(WORLD.getTrees()).stream()
        			.min(new Comparator<Tree>() {
						public int compare(Tree o1, Tree o2) {
							return p.getDistanceTo(o1) - p.getDistanceTo(o2) < 0 ? -1 : 1;
						}
        			})
        			.orElse((Tree)null);
    	}
    	
    	return res;
    }
    
    
    void avoidMagicAttack() {
    	avoidProjectile = checkProjectiles();
    	pointToAvoidMagicAttack = getPointToAvoidMagicAttack();

        if (pointToAvoidMagicAttack != null && avoidProjectile != null) {
        	targetPoint = pointToAvoidMagicAttack;
        }
    }
    
    
    void chooseLane() {
        if (WORLD.getTickIndex() < 400 && !GAME.isRawMessagesEnabled()) {
        	lane = LaneType.MIDDLE;
        	return;
        }
        if (WORLD.getTickIndex() < 400 && GAME.isRawMessagesEnabled()) {
        	return;
        }
    	if (goToLane) 
    		return;
        
        double topDist = POWER_MAP.getNearestTopEnemy().getCenter().getDistanceTo(HOME_POINT.point);
        double bottomDist = POWER_MAP.getNearestBottomEnemy().getCenter().getDistanceTo(HOME_POINT.point);
        double middleDist = POWER_MAP.getNearestMiddleEnemy().getCenter().getDistanceTo(HOME_POINT.point);
        
    	Line l = new Line(HOME_POINT.point, ENEMY_HOME_POINT.point);
    	HALF half;
    	if (l.getRelativePointPosition(new Point2D(SELF)) + l.getRelativePointPosition(new Point2D(0,0)) != 0)
			half = HALF.TOP;
		else
			half = HALF.BOTTOM;
    		
		PowerRect bottom = POWER_MAP.getNextBottom();
		PowerRect top = POWER_MAP.getNextTop();
		PowerRect middle = POWER_MAP.getNextMiddle();
		
		int indBottom = POWER_MAP.bottomRects.indexOf(bottom);
		int indTop = POWER_MAP.topRects.indexOf(top);
		int indMiddle = POWER_MAP.middleRects.indexOf(middle);
        

        
		// ========== I on Base ===========
		// find nearest by lane
        if (POWER_MAP.middleRects.get(0).contains(new Point2D(SELF))) {
	        if (topDist <= bottomDist && topDist <= middleDist && indTop <= 3)
	        	lane = LaneType.TOP;
	        else if (bottomDist <= topDist && bottomDist <= middleDist && indBottom <= 3)
	        	lane = LaneType.BOTTOM;
	        else 
	        	lane = LaneType.MIDDLE;
	        return;
        }

        
        // ========== command play ========
        
        if (GAME.isRawMessagesEnabled())
        	return;
        
        
        // ========== I on Lane ==========
        
        LaneType myLane = POWER_MAP.getRectLane(POWER_MAP.getMyRect());
        
        if (myLane != null) {
        	if (enemyBuildingsByLane.get(myLane) > 0)
        		return;
        	
        	// buildings on mylane == 0
        	if (myLane != LaneType.MIDDLE) {
        		int ind = myLane == LaneType.TOP ? indTop : indBottom;
        		if (enemyBuildingsByLane.get(LaneType.MIDDLE) == 1 
        				&& indMiddle >= 7
        				&& (ind >= 6 || ind == -1) ) {
        			lane = LaneType.MIDDLE;
        			return;
        		}
        	}
        	
        	// i on mid lane
        	else {
        		if (indMiddle <= 5 || POWER_MAP.getRectsByLane(LaneType.MIDDLE).indexOf(POWER_MAP.getMyRect()) <= 5)
        			return;
        		if (/*enemyBuildingsByLane.get(LaneType.TOP) > 0 && indTop >= 4*/
        				enemyBuildingsByLane.get(LaneType.TOP) == 1 
        				&& indMiddle >= 7
        				&& (indTop >= 6 || indTop == -1)
        		) {
        			lane = LaneType.TOP;
        			return;
        		}
        		if (/*enemyBuildingsByLane.get(LaneType.BOTTOM) > 0 && indBottom >= 4*/
        				enemyBuildingsByLane.get(LaneType.BOTTOM) == 1 
        				&& indMiddle >= 7
        				&& (indBottom>= 6 || indBottom == -1)
        		) {
        			lane = LaneType.BOTTOM;
        			return;
        		}
        	}
        	
        	return;
        }
        
        
        // ============= I on neutral =========
        
        boolean onNeutralLane = false;
        if (POWER_MAP.neutralRects.contains(POWER_MAP.getMyRect()))
        	onNeutralLane = true;
        LaneType wingLane;
        
        if (half == HALF.TOP) {
        	double wing = new Waypoint(new Point2D(SELF)).getDistanceTo(new Waypoint(top.getCenter()));
        	double mid = new Waypoint(new Point2D(SELF)).getDistanceTo(new Waypoint(middle.getCenter()));
        	wingLane = LaneType.TOP;
        	
        	if (wing > mid)
        		lane = LaneType.MIDDLE;
        	else
        		lane = LaneType.TOP;
        }
        
        else {	// half BOTTOM
        	double wing = new Waypoint(new Point2D(SELF)).getDistanceTo(new Waypoint(bottom.getCenter()));
        	double mid = new Waypoint(new Point2D(SELF)).getDistanceTo(new Waypoint(middle.getCenter()));
        	wingLane = LaneType.BOTTOM;
        	
        	if (wing > mid)
        		lane = LaneType.MIDDLE;
        	else
        		lane = LaneType.BOTTOM;
        }
        
    	if (onNeutralLane && enemyWizardsCnt == 5) {
    		if (enemyWizardsByLane.get(wingLane).size() == 0) {
    			lane = wingLane;
    			return;
    		}
    		if (enemyWizardsByLane.get(LaneType.MIDDLE).size() == 0) {
    			lane = LaneType.MIDDLE;
    			return;
    		}
    		
    		double wingFriend = friendWizardsByLane.get(wingLane).stream()
    				.filter(x -> x.getId() != SELF.getId())
    				.mapToDouble(x -> {
    					if (x.getSkills().length >= 5)
    						return 2.5;
    					else
    						return 1.;
    				})
    				.sum();
    		double wingEnemy = enemyWizardsByLane.get(wingLane).stream()
    				.mapToDouble(x -> {
    					if (x.getSkills().length >= 5)
    						return 2.5;
    					else
    						return 1.;
    				})
    				.sum();
    		double middleFriend = friendWizardsByLane.get(LaneType.MIDDLE).stream()
    				.filter(x -> x.getId() != SELF.getId())
    				.mapToDouble(x -> {
    					if (x.getSkills().length >= 5)
    						return 2.5;
    					else
    						return 1.;
    				})
    				.sum();
    		double middleEnemy = enemyWizardsByLane.get(LaneType.MIDDLE).stream()
    				.mapToDouble(x -> {
    					if (x.getSkills().length >= 5)
    						return 2.5;
    					else
    						return 1.;
    				})
    				.sum();
    		
    		double selfExp = SELF.getSkills().length >= 5 ? 2.5 : 1;
    		double friendExp = selfExp + wingFriend;
    		double myWingExp = selfExp / friendExp * friendExp / wingEnemy;
    		friendExp = selfExp + middleFriend;
    		double myMiddleExp = selfExp / friendExp * friendExp / middleEnemy;
    		
    		if (myMiddleExp >= myWingExp)
    			lane = LaneType.MIDDLE;
    		else 
    			lane = wingLane;
    	}

    }
    
    
    
    void chooseEnemyTarget() {	
    	Comparator<LivingUnit> DPMCmp = new Comparator<LivingUnit>() {
			public int compare(LivingUnit u1, LivingUnit u2) {
				return getUnitDPM(u1) > getUnitDPM(u2) ? 1 : -1;
			}
		};
		
		
    	// =========== FIREBALL ===============

    	
    	if (isFireballEnable 
    			&& SELF.getRemainingCooldownTicksByAction()[4] == 0
    			&& SELF.getMana() >= GAME.getFireballManacost() ) {
    		
    		// choose Wizard or Building
    		enemyTarget = enemiesInCastRange.stream()
    				.filter(x -> (x instanceof Wizard) || (x instanceof Building))
    				.filter(x -> x.getLife() > wizardMagicDamage.get(SELF))
    				// command play
    				.filter(x -> {
    					if (!GAME.isRawMessagesEnabled())
    						return true;
    					
    					Line line = new Line(new Point2D(x), new Point2D(SELF));
    					Point2D point = line.start.clone();
    					Vector v = line.direction.clone();
    					v.normalize(x.getRadius() + GAME.getFireballRadius());
    					point.moveOnVector(v);
    					
    					for (Wizard w: friendWizards) {
    						Building b = null;
    						for (Building bb: enemiesBuildingsList)
    							if (bb.getDistanceTo(w) <= bb.getAttackRange())
    								b = bb;
    						double life = 40;
    						if (b != null)
    							life += b.getDamage();
    						if (w.getLife() < life 
    								&& point.getDistanceTo(new Point2D(w)) <= GAME.getFireballExplosionMinDamageRange())
    							return false;
    					}
    					return true;
    				})
    				.max(DPMCmp)
    				.orElse((LivingUnit)null);
    		
    		// choose Wizard or Building out of cast range
    		if (enemyTarget == null)
	    		enemyTarget = enemyUnits.stream()
	    				.filter(x -> {
	    					if (x instanceof Building)
	    						return new Point2D(SELF).getDistanceTo(new Point2D(x)) <= wizardCastRange.get(SELF) + 99;
	    					if (x instanceof Wizard)
	    						return new Point2D(SELF).getDistanceTo(new Point2D(x)) <= wizardCastRange.get(SELF) + 50;
	    					return false;
	    				})
	    				// command play
	    				.filter(x -> {
	    					if (!GAME.isRawMessagesEnabled())
	    						return true;
	    					
	    					Line line = new Line(new Point2D(x), new Point2D(SELF));
	    					Point2D point = line.start.clone();
	    					Vector v = line.direction.clone();
	    					v.normalize(x.getRadius() + GAME.getFireballRadius());
	    					point.moveOnVector(v);

	    					for (Wizard w: friendWizards) {
	    						Building b = null;
	    						for (Building bb: enemiesBuildingsList)
	    							if (bb.getDistanceTo(w) <= bb.getAttackRange())
	    								b = bb;
	    						double life = 40;
	    						if (b != null)
	    							life += b.getDamage();
	    						
	    						if (w.getLife() < life 
	    								&& point.getDistanceTo(new Point2D(w)) <= GAME.getFireballExplosionMinDamageRange())
	    							return false;
	    					}
	    					return true;
	    				})
	    				.max(DPMCmp)
	    				.orElse((LivingUnit)null);
    		
    		// choose from threatenedMe
    		if (enemyTarget == null && SELF.getMana() > SELF.getMaxMana() * 0.5)
    		    enemyTarget = threatenedMeUnits.stream()
    				.filter(x -> x.getLife() > wizardMagicDamage.get(SELF))
    				// command play
    				.filter(x -> {
    					if (!GAME.isRawMessagesEnabled())
    						return true;
    					
    					Line line = new Line(new Point2D(x), new Point2D(SELF));
    					Point2D point = line.start.clone();
    					Vector v = line.direction.clone();
    					v.normalize(x.getRadius() + GAME.getFireballRadius());
    					point.moveOnVector(v);
    					
    					for (Wizard w: friendWizards) {
    						if (w.getLife() < 30 
    								&& point.getDistanceTo(new Point2D(w)) <= GAME.getFireballExplosionMinDamageRange())
    							return false;
    					}
    					return true;
    				})
    				.max(DPMCmp)
    				.orElse((LivingUnit)null);
    		
    		// choose from others if I have a lot of mana
    		if (enemyTarget == null && SELF.getMana() > SELF.getMaxMana() * 0.9)
	    		    enemyTarget = enemiesInCastRange.stream()
	    				.filter(x -> x.getLife() > wizardMagicDamage.get(SELF))
	    				// command play
	    				.filter(x -> {
	    					if (!GAME.isRawMessagesEnabled())
	    						return true;
	    					
	    					Line line = new Line(new Point2D(x), new Point2D(SELF));
	    					Point2D point = line.start.clone();
	    					Vector v = line.direction.clone();
	    					v.normalize(x.getRadius() + GAME.getFireballRadius());
	    					point.moveOnVector(v);
	    					
	    					for (Wizard w: friendWizards) {
	    						if (w.getLife() < 30 
	    								&& point.getDistanceTo(new Point2D(w)) <= GAME.getFireballExplosionMinDamageRange())
	    							return false;
	    					}
	    					return true;
	    				})
	    				.max(DPMCmp)
	    				.orElse((LivingUnit)null);
    		
    		if (enemyTarget != null) {
    			actionType = ActionType.FIREBALL;
    			return;
    		}
    	}
		
		
    	// =========== FROST_BOLT ===============

    	
    	if (isFrostBoltEnable 
    			&& SELF.getRemainingCooldownTicksByAction()[3] == 0
    			&& SELF.getMana() >= GAME.getFrostBoltManacost() ) {
    		
    		// threatened me that cannot avoid
    		enemyTarget = threatenedMeUnits.stream()
    				.filter(x -> !(x instanceof Building))
    				.filter(x -> isUnitInCastRange(x))
    				.filter(x -> {
    					if (!(x instanceof Wizard))
    						return true;
    					double dist = SELF.getDistanceTo(x);
    					double range = wizardCastRange.get(SELF) + x.getRadius() + GAME.getFrostBoltRadius();
    					return (int)(range / GAME.getFrostBoltSpeed()) < (int)((range - dist) / wizardBackwardSpeed.get((Wizard)x));
    				})
    				.filter(x -> x.getLife() > wizardMagicDamage.get(SELF))
    				.max(DPMCmp)
    				.orElse((LivingUnit)null);
    		
    		// threatened me
    		if (enemyTarget == null)
	    		enemyTarget = threatenedMeUnits.stream()
	    				.filter(x -> !(x instanceof Building))
	    				.filter(x -> isUnitInCastRange(x))
	    				.filter(x -> x.getLife() > wizardMagicDamage.get(SELF))
	    				.max(DPMCmp)
	    				.orElse((LivingUnit)null);
    		
    		// in target me that cannot avoid
    		if (enemyTarget == null)
    		    enemyTarget = enemiesInCastRange.stream()
    				.filter(x -> {
    					if (!(x instanceof Wizard))
    						return true;
    					double dist = SELF.getDistanceTo(x);
    					double range = wizardCastRange.get(SELF) + x.getRadius() + GAME.getFrostBoltRadius();
    					return (int)(range / GAME.getFrostBoltSpeed()) < (int)((range - dist) / wizardBackwardSpeed.get((Wizard)x));
    				})
    				.max(DPMCmp)
    				.orElse((LivingUnit)null);
    		
    		// in target if a lot of mana
    		if (enemyTarget == null && SELF.getMana() > SELF.getMaxMana() * 0.9)
	    		    enemyTarget = enemiesInCastRange.stream()
	    				.filter(x -> x.getLife() > wizardMagicDamage.get(SELF))
	    				.max(DPMCmp)
	    				.orElse((LivingUnit)null);

    		
    		if (enemyTarget != null) {
    			actionType = ActionType.FROST_BOLT;
    			return;
    		}
    	}
    	
    	
    	// =======================================
    	
    	int magicCD = SELF.getRemainingCooldownTicksByAction()[2];
    	int staffCD = SELF.getRemainingCooldownTicksByAction()[1];
    	
    	
    	Comparator<LivingUnit> rotateCmp = new Comparator<LivingUnit>() {
			public int compare(LivingUnit u1, LivingUnit u2) {
				return Math.abs(SELF.getAngleTo(u1) / wizardRotateSpeed.get(SELF)) < 
						Math.abs(SELF.getAngleTo(u2) / wizardRotateSpeed.get(SELF)) ? -1 : 1;
			}
    	};
    	
    	
    	// ============== STAFF ==================
    	
    	if (staffCD < magicCD) {
    		enemyTarget = threatenedMeUnits.stream()
    				.filter(x -> enemiesInStaffRange.contains(x))
    				.min(rotateCmp)
    				.orElse((LivingUnit)null);
    		
    		if (enemyTarget == null)
    			enemyTarget = enemiesInStaffRange.stream()
						.min(rotateCmp)
						.orElse((LivingUnit)null);
    		
    		if (enemyTarget == null)
    			enemyTarget = obstacleTarget;
    		
    		if (  enemyTarget == null 
    				|| (enemyTarget != null && Math.abs(SELF.getAngleTo(enemyTarget) / wizardRotateSpeed.get(SELF)) > magicCD) )
    			enemyTarget = null;
    		else {
    			actionType = ActionType.STAFF;
    			return;
    		}
    	}
    	
    	
    	// ================ MAGIC MISSILE ===============
    	
    	actionType = ActionType.MAGIC_MISSILE;
        enemyTarget = getUnitWithLowestHP(threatenedMeUnits.stream()
	     		    .filter(unit -> enemiesInCastRange.contains(unit))
	   				.filter(x -> {
						if (!(x instanceof Wizard))
							return true;
						double dist = SELF.getDistanceTo(x);
						double range = wizardCastRange.get(SELF) + x.getRadius() + GAME.getFrostBoltRadius();
						return (int)(range / GAME.getFrostBoltSpeed()) < (int)((range - dist) / wizardBackwardSpeed.get((Wizard)x));
					})
	     		   .collect(Collectors.toList())
     		   );
        
        if (enemyTarget == null)
        	enemyTarget = getUnitWithLowestHP(enemiesInCastRange);
        
        
        if (enemyTarget == null) {
        	enemyTarget = obstacleTarget;
        }
    }
    
    
    
    void doMoveStaff() {
    	
    	LaneType myLane = POWER_MAP.getRectLane(POWER_MAP.getMyRect());
		PowerRect myRect = POWER_MAP.getMyRect();
    	
		
    	// ================ hold right sight on mid ========================
    	
    	if (!goToBonus && !goToLane) {
    		if ( myLane == LaneType.MIDDLE ) {
    			int ind = POWER_MAP.middleRects.indexOf(myRect);
            	if ( ( /*(ind >= 3 && ind <= 8 && enemyBuildingsByLane.get(LaneType.MIDDLE) >= 2) 
            				||*/ ind >= 6 && ind <= 8  && enemyBuildingsByLane.get(LaneType.TOP) >= 1 )
            			&& POWER_MAP.getPointLane(targetPoint) == LaneType.MIDDLE ) {
            		
            		Line l = new Line(new Point2D(800, 3600), new Point2D(3600, 800));
            		double dist = new Point2D(SELF).getDistanceTo(l);
            		if (dist > 150) {
	            		targetPoint.x = SELF.getX() + (dist - 100) * Math.sqrt(2);
	            		targetPoint.y = SELF.getY();
	            		return;
            		}
            	}
    		}
    	}
    	
    	
    	// ================ hold right sight on bottom ========================
    	
    	if (!goToBonus && !goToLane) {
    		if ( myLane == LaneType.BOTTOM ) {
    			int ind = POWER_MAP.bottomRects.indexOf(myRect);
            	if ( ind >= 5 && enemyBuildingsByLane.get(LaneType.MIDDLE) >= 1 ) {
            		
            		if (SELF.getX() < 3800) {
	            		targetPoint.x = SELF.getX() + 100;
	            		targetPoint.y = SELF.getY() - 100;
	            		return;
            		}
            	}
    		}
    	}
    	
    	
    	// ============= hold close to Building on on HP =====================
    	
    	for (Building b: enemiesBuildingsList) {
	    	if (b.getLife() < 100
	    			&& SELF.getLife() > b.getDamage() + 1
	    			&& SELF.getDistanceTo(b) > 595 && SELF.getDistanceTo(b) < 800 )
	    		targetPoint = new Point2D(b);
    	}
    	
    	
    	// ================= command play ====================
    	// hold distance to building if friend wizard will attack it with fireball
    	if (GAME.isRawMessagesEnabled()) {
    		for (Building b: enemiesBuildingsList) {
    			if (getDistanceBetween(SELF,  b) < GAME.getFireballExplosionMaxDamageRange()) {
    				for (Wizard w: friendWizards)
    					if (new Point2D(w).getDistanceTo(b) < wizardCastRange.get(w)
    							&& Arrays.asList(w.getSkills()).contains(SkillType.FIREBALL)
    							&& w.getRemainingCooldownTicksByAction()[4] < 20
    					) {
    						Line line = new Line(new Point2D(b), new Point2D(w));
    						Point2D point = line.start.clone();
    						Vector v = line.direction.clone();
    						v.normalize(b.getRadius() + GAME.getFireballRadius());
    						point.moveOnVector(v);
    						
    						if (new Point2D(SELF).getDistanceTo(point) <= GAME.getFireballExplosionMaxDamageRange()) {
    							Line l = new Line(line.start, new Point2D(SELF));
    							Vector vv = l.direction.clone();
    							vv.normalize(10);
    							targetPoint = new Point2D(SELF);
    							targetPoint.moveOnVector(vv);
    							return;
    						}
    					}
    			}
    		}
    	}
    	
    }
    
    
    
    void learnSkill() {
        initSkillsQueue();
        
        if (myRole == null)
        	return;
        
    	if (SELF.getLevel() > SELF.getSkills().length) {
	    	/*SkillType skill = SKILLS_TO_LEARN.poll();*/
    		SkillType skill = roleSkills.get(myRole).poll();
	    	
	    	if (skill == null) 
	    		return;
	    	
	    	MOVE.setSkillToLearn(skill);
	    	
	    	/*for (SkillType s: SKILLS_QUEUE)
	    		SKILLS_LISTING.get(s).remove(skill);*/
	    	
	    	if (skill == SkillType.FROST_BOLT)
	    		isFrostBoltEnable = true;
	    	if (skill == SkillType.FIREBALL)
	    		isFireballEnable = true;
	    	if (skill == SkillType.HASTE)
	    		isHasteEnable = true;
	    	if (skill == SkillType.SHIELD)
	    		isShieldEnable = true;
	    	
    	}
    }
    
    
    
    Point2D getPointToAvoidMinionAppearLock() {
    	// ======= command play =========
    	if (GAME.isRawMessagesEnabled()) {
    		/* move to FACTION_BASE if go too far from point minions appears */
    		if (SELF.getX() > 3000 && SELF.getY() < 1000) {
    			boolean wizardIn = false;
    			/*for (Wizard w: enemyWizards)
    				if (POWER_MAP.middleRects.get(8).contains(new Point2D(w))) {
    					wizardIn = true;
    					break;
    				}*/

    			if (!wizardIn)
    				return null;
    		}
    	}
    	
    	
    	// =================
    	
    	LaneType myLane = POWER_MAP.getRectLane(POWER_MAP.getMyRect());

    	Point2D target = null;
    	
    	if (myLane == LaneType.TOP)
    		target = new Point2D(4000 - 1200, 200);
    	else if (myLane == LaneType.BOTTOM)
    		target = new Point2D(3800, 1200);
    	else if (myLane == LaneType.MIDDLE)
    		target = new Point2D(4000 - 1200 / Math.sqrt(2), 1200 / Math.sqrt(2));
    	
    	
    	if (target == null
    			|| ENEMY_HOME_POINT.point.getDistanceTo(target) < ENEMY_HOME_POINT.point.getDistanceTo(new Point2D(SELF)))
    		return null;
    	
    	
    	double ticks = new Point2D(SELF).getDistanceTo(target) / wizardBackwardSpeed.get(SELF);
    	double remainingTicks = GAME.getFactionMinionAppearanceIntervalTicks() - WORLD.getTickIndex() % GAME.getFactionMinionAppearanceIntervalTicks();
    	
    	if (ticks > remainingTicks)
    		return target;
    	
    	return null;
    }
    
    
    
    Point2D getNextPointOnLane(LaneType lane) {
    	if (ENEMY_HOME_POINT.point.getDistanceTo(SELF) 
				< ENEMY_HOME_POINT.point.getDistanceTo(POWER_MAP.getRectsByLane(lane)
						.get(POWER_MAP.getRectsByLane(lane).size() - 1).getCenter()) )
				return ENEMY_HOME_POINT.point;
    	
    	
    	// I on lane
    	if (POWER_MAP.getPointLane(new Point2D(SELF)) == lane) {
    		return POWER_MAP.getNextByLane(lane).getCenter();
    	}
    	
    	// find near to enemy home unit or farest wizardCastRange on lane
		
		List<Point2D> points = new ArrayList<Point2D>();
		/*for (LivingUnit w: enemyWizards) {
			Circle c = new Circle(w);
			c.r = getUnitRange(w);
			points.addAll(new Line(HOME_POINT.point, targetWaypointByLane.get(lane).point)
					.getCircleIntersection(c));
			points.addAll(new Line(ENEMY_HOME_POINT.point, targetWaypointByLane.get(lane).point)
					.getCircleIntersection(c));
		}*/
		
		Comparator<Point2D> cmp = new Comparator<Point2D>() {
			public int compare(Point2D p1, Point2D p2) {
				return ENEMY_HOME_POINT.point.getDistanceTo(p1) < ENEMY_HOME_POINT.point.getDistanceTo(p2) ? -1 : 1; 
			}
		};
	
    	Point2D nearer = friendUnitsExcludeMe.stream()
    		.filter(x -> POWER_MAP.getPointLane(new Point2D(x)) == lane)
    		.filter(x -> POWER_MAP.getPointRect(new Point2D(x)) != POWER_MAP.middleRects.get(8))
    		.map(x -> {
    			Line l1 = new Line(HOME_POINT.point, targetWaypointByLane.get(lane).point);
    			Line l2 = new Line(ENEMY_HOME_POINT.point, targetWaypointByLane.get(lane).point);
    			
    			if (new Point2D(x).getDistanceTo(l1) < new Point2D(x).getDistanceTo(l2))
    				return new Point2D(x).getProjectionToLine(l1);
    			else
    				 return new Point2D(x).getProjectionToLine(l2);
    		})
    		.min(cmp)
    		.orElse((Point2D)null);
    	
    	if (nearer != null) {
    		points.add(nearer);
    	}
    	
    	Point2D res = points.stream().max(cmp).orElse((Point2D)null);

    	if (res == null || getWizardLife(SELF) < SELF.getMaxLife() * 0.5)
    		res = POWER_MAP.getNextSafePointByLane(lane);

    	return res;
    }
    
    
    
    enum HALF {TOP, BOTTOM};
    
    
    /**
     * 
     */
    void estimateDefeat() {
    	if (POWER_MAP.middleRects.get(0).enemyPowerInRect > 0 && new Point2D(SELF).getDistanceTo(HOME_POINT.point) < 3400 )
    		isDefeat = true;

    	if (GAME.isRawMessagesEnabled()) {
    		if (POWER_MAP.bottomRects.get(0).enemyPowerInRect > 0) {
    			Wizard w = SELF;
    			for (Wizard f: friendWizards)
    				if (POWER_MAP.bottomRects.get(0).getCenter().getDistanceTo(f) 
    						< POWER_MAP.bottomRects.get(0).getCenter().getDistanceTo(SELF))
    					w = f;
    			if (w == SELF) 
    				if (!POWER_MAP.bottomRects.get(0).contains(SELF) && !POWER_MAP.middleRects.get(0).contains(SELF)) {
    					isDefeat = true;
    					lane = LaneType.BOTTOM;
    				}
    		}
    		
    		if (POWER_MAP.middleRects.get(1).enemyPowerInRect > 0) {
    			Wizard w = SELF;
    			for (Wizard f: friendWizards)
    				if (POWER_MAP.middleRects.get(1).getCenter().getDistanceTo(f) 
    						< POWER_MAP.middleRects.get(1).getCenter().getDistanceTo(SELF))
    					w = f;
    			if (w == SELF)
    				if (!POWER_MAP.middleRects.get(1).contains(SELF) && !POWER_MAP.middleRects.get(0).contains(SELF)) {
    					isDefeat = true;
    					lane = LaneType.MIDDLE;
    				}
    		}
    		
    		if (POWER_MAP.topRects.get(0).enemyPowerInRect > 0) {
    			Wizard w = SELF;
    			for (Wizard f: friendWizards)
    				if (POWER_MAP.topRects.get(0).getCenter().getDistanceTo(f) 
    						< POWER_MAP.topRects.get(0).getCenter().getDistanceTo(SELF))
    					w = f;
    			if (w == SELF)
    				if (!POWER_MAP.topRects.get(0).contains(SELF) && !POWER_MAP.middleRects.get(0).contains(SELF)) {
    					isDefeat = true;
    					lane = LaneType.TOP;
    				}
    		}
    		
    	}
    	
    	if ( POWER_MAP.middleRects.get(0).contains(SELF) 
    			&& (isDefeat || (!isDefeat && isDefeatLane))) {
    		isDefeat = false;
    		isDefeatLane = true;
    		
    		
    		if (POWER_MAP.middleRects.get(0).enemyPowerInRect > 0) {
    			LivingUnit min = enemyUnits.stream()
    					.filter(x -> POWER_MAP.middleRects.get(0).contains(new Point2D(x)))
    					.min(new Comparator<LivingUnit>() {
		    				public int compare(LivingUnit u1, LivingUnit u2) {
		    					return new Point2D(0, 4000).getDistanceTo(u1) < new Point2D(0, 4000).getDistanceTo(u2) ? -1 : 1;
		    				}
    					})
    					.orElse((LivingUnit)null);
    			
    			if (min.getX() < 400)
    				lane = LaneType.TOP;
    			else if (min.getY() > 3600)
    				lane = LaneType.BOTTOM;
    			else lane = LaneType.MIDDLE;
    		}
    	
    		
    		else {
	    		makeLaneByNearestEnemy();
    		}
    	}

    	
    	if (isDefeatLane) {
    		for (PowerRect r: POWER_MAP.rects)
    			if (r.contains(targetWaypointByLane.get(lane).point)) {
    				if (r.getPowerInRect() > 0 || targetWaypointByLane.get(lane).point.getDistanceTo(SELF) < 1250)
    					isDefeatLane = false;
    				break;
    			}
    	}
    	
    }
    
    
    
    void makeLaneByNearestEnemy() {
		double bottomDist = Math.min(POWER_MAP.getNearestBottomEnemy().getCenter().getDistanceTo(HOME_POINT.point),
				POWER_MAP.getFurtherBottomFriend().getCenter().getDistanceTo(HOME_POINT.point) );
		double topDist = Math.min(POWER_MAP.getNearestTopEnemy().getCenter().getDistanceTo(HOME_POINT.point),
				POWER_MAP.getFurtherTopFriend().getCenter().getDistanceTo(HOME_POINT.point) );
		double middleDist = Math.min(POWER_MAP.getNearestMiddleEnemy().getCenter().getDistanceTo(HOME_POINT.point),
				POWER_MAP.getFurtherMiddleFriend().getCenter().getDistanceTo(HOME_POINT.point) );

		if (bottomDist <= topDist && bottomDist <= middleDist)
			lane = LaneType.BOTTOM;
		else if (topDist <= bottomDist && topDist <= middleDist)
			lane = LaneType.TOP;
		else
			lane = LaneType.MIDDLE;
    }
    
    
    /**
     * 
     */
    void estimateLifeAction() {
    	double selfLife = getWizardLife(SELF);
    	
    	// low HP
    	if (selfLife < SELF.getMaxLife() * LOW_HP_FACTOR) {
    		for (LivingUnit u: enemiesWithAggressiveMinions)
    			if (inUnitRange(u, getUnitRange(u) + 100, SELF)) {
    				isRetreat = true;
    				break;
    			}
    		
    		// command play
    		if (GAME.isRawMessagesEnabled()) {
    			if (SELF.getX() > 3200 && SELF.getY() < 800 && enemyTarget instanceof Building)
    				isRetreat = false;
    		}
        }
    	
    	if (SELF.getLife() < 28) {
    		for (Wizard w: enemyWizards) {
    			if (new Point2D(w).getDistanceTo(SELF) <= wizardCastRange.get(w) + 30) {
    				isRetreat = true;
    				isTurnRetreat = true;
    				break;
    			}
    		}
    	}
    	
    	if (!GAME.isRawMessagesEnabled()) {
	    	// HP lower then units Damage
	    	double dmg = 0;
	    	for (LivingUnit u: enemiesWithAggressiveMinions) {
	    		double range = getUnitRange(u);
	    		if (u instanceof Building || u instanceof Minion) 
	    			range += 10;
	    		else 
	    			range += 100;
	    		if (inUnitRange(u, range, SELF))
	    			dmg += getUnitDamage(u);
	    	}
	
	    	if (dmg >= getWizardLife(SELF) )
	    		isRetreat = true;
    	}

    	// HOLD_DISTANCE depends on HP
    	double min = 0.96;
    	double max = 0.98;
    	double myCastRange = wizardCastRange.get(SELF);
    	if (SELF.getLife() <= 26) {
    		for (Wizard w: enemyWizards)
    			if (hasSkill(w, SkillType.FIREBALL))
    				max = Math.max(max, (wizardCastRange.get(w) + 50) / myCastRange);
    		HOLD_DISTANCE_FACTOR = max;
    	} 
    	else
    		HOLD_DISTANCE_FACTOR = max + (-max + min) * selfLife / SELF.getMaxLife();
    }
    
    
    void go() {
		if (goToBonus)
			makeNearestPath(new Waypoint(targetPoint), waypointsToBonus);
		else 
			makeNearestPath(new Waypoint(targetPoint), waypointsToMove);
		
    	if (!goToBonus && !POWER_MAP.isWaySafe(waypointsToMove))
    		isWayDirect = true;
    	
    	Point2D to = getNextWaypoint().point;
    	
    	
    	// ===== norm point on wing corner ======
    	if (lane != LaneType.MIDDLE) {
    		List<PowerRect> list = POWER_MAP.getRectsByLane(lane);
    		if (list.indexOf(POWER_MAP.getMyRect()) == list.size() / 2) {
    			Point2D next = list.get(4).getCenter();
    			if (to.x == next.x && to.y == next.y) {
    				if ( (lane == LaneType.TOP && SELF.getY() > 450 && SELF.getX() < 450) )
    					to = new Point2D(800, 300);
    				if ((lane == LaneType.BOTTOM && SELF.getX() < 3550 && SELF.getY() > 3550))
    					to = new Point2D(3700, 3200);
    			}
    		}
    	}
    	
    	// =======================================
    	
    	if (targetTurn == null) 
    		targetTurn = to;
    	targetPoint = to;
    	

    	goTo(targetPoint);
    	
    	
    	// ========== don't move under projectile ===========
    	Point2D target = new Point2D(SELF.getX() + Math.signum(targetPoint.getX() - SELF.getX()) * wizardForwardSpeed.get(SELF), 
    			SELF.getY() + Math.signum(targetPoint.getY() - SELF.getY()) * wizardForwardSpeed.get(SELF));
    	if (!isRetreat && avoidProjectile == null) {
	    	for (Projectile p: projectiles) {
	    		if (p.getType() == ProjectileType.DART)
	    			continue;
	    		
	    		Point2D start = projectilesStart.get(p);
	    		Line line = new Line(start, new Point2D(start.x + p.getSpeedX(), start.y + p.getSpeedY()));
	    		
	    		// aside
	    		if (new Point2D(SELF).getDistanceTo(line) >= SELF.getRadius() + 0.1 
	    				&& target.getDistanceTo(line) <= SELF.getRadius() + 0.1) {
	    			MOVE.setSpeed(0);
	    			MOVE.setStrafeSpeed(0);
	    			break;
	    		}

	    		// backward
	    		double radius = p.getRadius();
	    		LivingUnit owner = getUnitById(p.getOwnerUnitId());
	    		double range = 0;
	    		if (owner == null)
	    			range = 600;
	    		else
	    			range = wizardCastRange.get((Wizard)owner);
	    		double distToMe = start.getDistanceTo(SELF) - radius - 0.5;
	    		double distToTargetPoint = start.getDistanceTo(target) - SELF.getRadius() - radius - 0.5;
	    		
	    		Circle c = new Circle(SELF);
	    		c.r += p.getRadius() + 0.1;
	    		if (range < distToMe && distToTargetPoint <= range
	    				&& line.getCircleIntersection(c).size() != 0) {
	    			MOVE.setSpeed(0);
	    			MOVE.setStrafeSpeed(0);
	    			break;
	    		}

	    	}
    	}
    }
    
    
    boolean attackStaff(CircularUnit enemyTarget) {
    	if (enemyTarget == null)
    		return false;
    	
    	if ( SELF.getRemainingActionCooldownTicks() == 0 
			&& SELF.getRemainingCooldownTicksByAction()[1] == 0 
			&& isUnitInStaffSector(SELF, enemyTarget)) {
	        		MOVE.setAction(ActionType.STAFF);
	        		return true;
	    	}
    	
    	return false;
    }
    
    
    boolean attackMagicMissile(CircularUnit enemyTarget) {
    	if (enemyTarget == null)
    		return false;
    	
    	if ( SELF.getRemainingActionCooldownTicks() == 0
    			&& SELF.getRemainingCooldownTicksByAction()[2] == 0 
				&& SELF.getMana() >= GAME.getMagicMissileManacost() 
				&& isUnitInCastSector(SELF, enemyTarget) ) {
    		
            double distanceToTargetEnemy = SELF.getDistanceTo(enemyTarget);
            double angle = SELF.getAngleTo(enemyTarget);
        	MOVE.setAction(ActionType.MAGIC_MISSILE);
            MOVE.setCastAngle(angle);
            MOVE.setMinCastDistance(distanceToTargetEnemy - enemyTarget.getRadius() + GAME.getMagicMissileRadius());
            return true;
		}
    	
    	return false;
    }
    
    
    boolean attackFrostBolt(CircularUnit enemyTarget) {
    	if (enemyTarget == null)
    		return false;
    	
    	if ( SELF.getRemainingActionCooldownTicks() == 0
    			&& SELF.getRemainingCooldownTicksByAction()[3] == 0 
				&& SELF.getMana() >= GAME.getFrostBoltManacost() 
				&& isUnitInCastSector(SELF, enemyTarget) ) {
    		
            double distanceToTargetEnemy = SELF.getDistanceTo(enemyTarget);
            double angle = SELF.getAngleTo(enemyTarget);
        	MOVE.setAction(ActionType.FROST_BOLT);
            MOVE.setCastAngle(angle);
            MOVE.setMinCastDistance(distanceToTargetEnemy - enemyTarget.getRadius() + GAME.getFrostBoltRadius());
            return true;
		}
    	
    	return false;
    }
    
    
    boolean attackFireball(CircularUnit enemyTarget) {
    	if (enemyTarget == null)
    		return false;

    	if ( SELF.getRemainingActionCooldownTicks() == 0
    			&& SELF.getRemainingCooldownTicksByAction()[4] == 0 
				&& SELF.getMana() >= GAME.getFireballManacost() 
				&& isUnitInCastSector(SELF, enemyTarget) ) {
    		
            double distanceToTargetEnemy = SELF.getDistanceTo(enemyTarget);
            double angle = SELF.getAngleTo(enemyTarget);
        	MOVE.setAction(ActionType.FIREBALL);
            MOVE.setCastAngle(angle);
            if (distanceToTargetEnemy <= 100 && 101 - distanceToTargetEnemy < enemyTarget.getRadius() )
            	MOVE.setMinCastDistance(101);
            else
            	MOVE.setMinCastDistance(distanceToTargetEnemy - enemyTarget.getRadius() + GAME.getFireballRadius());
            return true;
		}
    	
    	return false;
    }
    
    
    void attack() {
    	//CircularUnit target = enemyTarget == null ? obstacleTarget : enemyTarget;

    	if (enemyTarget == null && obstacleTarget == null) 
    		return;
    	
    	
    	if (enemyTarget != null) {
    		
    		targetTurn = new Point2D(enemyTarget);
    		
    		
    		if (actionType == ActionType.FROST_BOLT) {
    			attackFrostBolt(enemyTarget);
    			return;
    		}
    		
    		
    		if (actionType == ActionType.FIREBALL) {
    			attackFireball(enemyTarget);
    			return;
    		}
    		
    		
    		if (actionType == ActionType.MAGIC_MISSILE) {
    			attackMagicMissile(enemyTarget);
    			return;
    		}
    		
    		
    		if (actionType == ActionType.STAFF) {
    			attackStaff(enemyTarget);
    		}
    		
    	}
        
    }

    
    
    void holdDistance() {
		
		// ========== if go to bonus calculate my power =============
	
		if ( goToBonus && targetBonus.getDistanceTo(SELF) < GAME.getWizardVisionRange()) {
			List<LivingUnit> enemyWizards = enemyUnits.stream()
					.filter(x -> x instanceof Wizard)
					.filter(x -> targetBonus.getDistanceTo(x) < GAME.getWizardCastRange())
					.collect(Collectors.toList());
			
			double DPM = 0, life = 0;
			for (LivingUnit w: enemyWizards) {
				DPM += getUnitDPM(w);
				life += w.getLife();
			}
			
			List<LivingUnit> friendWizards = friendUnits.stream()
					.filter(x -> x instanceof Wizard)
					.filter(x -> targetBonus.getDistanceTo(x) < GAME.getWizardVisionRange())
					.collect(Collectors.toList());
			
			double friendDPM = 0, friendLife = 0;
			for (LivingUnit w: friendWizards) {
				friendDPM += getUnitDPM(w);
				friendLife += w.getLife();
			}
			
			if (life / friendDPM > friendLife / DPM) {
				isRetreat = true;
				bonusExist.put(targetBonus, false);
			}
			
			return;
		}
    	
    	
		if (SELF.getLife() > SELF.getMaxLife() * 0.6) {
			HOLD_DISTANCE_FACTOR_CONST = 300;
			for (LivingUnit e: enemiesWithAggressiveMinions)
				for (LivingUnit u: friendUnitsExcludeMe)
					if (e != null && new Point2D(u).getDistanceTo(new Point2D(e)) < new Point2D(u).getDistanceTo(new Point2D(SELF))) {
						HOLD_DISTANCE_FACTOR_CONST = 60;
						break;
					}
		} else {
			HOLD_DISTANCE_FACTOR_CONST = 300;
		}
		
		
		
    	// hold distance more then enemy range with shortest range
    	// and HOLD_DISTANCE_FACTOR * selfRange to enemy whose range longer then my
    	if ( enemiesWithAggressiveMinions.stream()
    		.filter( x -> {   			
    			double unitRange = getUnitRange(x);
    			double selfRange = getUnitRange(SELF);

    			// ==================== MINIONS ========================
    			
    			if (/*unitRange + 50 < selfRange*/ x instanceof Minion) {
    				if (SELF.getLife() > SELF.getMaxLife() * 0.6)
    					return getDistanceToUnitFromCenter(x, SELF) < Math.max(HOLD_DISTANCE_FACTOR_CONST, 0);
    				else {
    					// command play
    					if (GAME.isRawMessagesEnabled()) {
	    					if (getDistanceToUnitFromCenter(x, SELF) <= unitRange
	    							&& friendWizardsByLane.get(lane).size() > enemyWizardsByLane.get(lane).size()
	    					) {
	    						LivingUnit minionTarget = SELF;
	    						
	    						for (LivingUnit u: friendUnits)
	    							if (u.getDistanceTo(x) < SELF.getDistanceTo(x)) {
	    								minionTarget = u;
	    								break;
	    							}
	    						
	        					if (minionTarget == SELF)
	        						return false;
	        					else 
	        						return true;
	    					}
    					}

    					return getDistanceToUnitFromCenter(x, SELF) < Math.max(HOLD_DISTANCE_FACTOR_CONST, unitRange + 15);
    				}
    			} 
    			
    			
    			// ====== command play =======
    			if (GAME.isRawMessagesEnabled()) {
    				if (isCommandLaneRush) {
    					boolean rush = SELF.getLife() > 70;
    					for (Wizard w: friendWizards) 
    						if (getWizardLife(w) > getWizardLife(SELF)) {
    							rush = false;
    							break;
    						}
    					
    					if (rush || SELF.getLife() > 70)
    						return false;
    				}
    			}
    			
    			
    			// ==================== BUILDINGS =====================
    			if (x instanceof Building) {
    				
    		    	// ============== command play =================
    		    	if (GAME.isRawMessagesEnabled()) {
    		    		if ( enemyWizardsCnt == 5 
    		    				&& (enemyWizardsByLane.get(lane).size() == 0
    		    					|| friendWizardsByLane.get(lane).size() / enemyWizardsByLane.get(lane).size() >= 5./3.)
    		    		) {
    		    			boolean rush = SELF.getLife() > ((Building)x).getDamage() + 20 ? true : false;
    		    			/*for (Wizard w: friendWizardsByLane.get(lane)) {
    		    				if (getWizardLife(w) > getWizardLife(SELF))
    		    					rush = false;
    		    			}*/
    		    			if (rush)
    		    				return false;
    		    		}
    		    	}
    		    	
    				
    				// ========== if I one on Lane ===============
    				if (enemyWizardsCnt == 5 
    						&& enemyTarget == x
    						&& lane == POWER_MAP.getPointLane(new Point2D(x))
    						&& SELF.getLife() >= ((Building)x).getDamage())
    					return false;
    				
    				
    				// ========== find building target ===========
    				LivingUnit buildingTarget = SELF;
    				for (LivingUnit u: friendUnits)
    					if ( x.getDistanceTo(u) <= ((Building)x).getAttackRange() ) {
    						if (u.getLife() > ((Building)x).getDamage()) {
    							if ( u.getLife() > ((Building)x).getDamage() 
    									&& (u.getLife() < buildingTarget.getLife()
    											|| buildingTarget.getLife() < ((Building)x).getDamage())
    							)
    								buildingTarget = u;
    						} else {
    							if (buildingTarget.getLife() < ((Building)x).getDamage()) {
    								if (u.getLife() > buildingTarget.getLife())
    									buildingTarget = u;
    							}
    						}
    					}
    				
    				
					if (GAME.isRawMessagesEnabled() && SELF.getX() > 3200 && SELF.getY() < 800)
						return false;

    				
    				// ============= low HP building =============
    		    	if (x.getLife() < 60
    		    			/*&& SELF.getLife() > ((Building)x).getDamage() + 1*/
    		    			&& ( buildingTarget != SELF || (SELF.getLife() > ((Building)x).getDamage()) )
    		    			&& SELF.getDistanceTo(x) > 595 && SELF.getDistanceTo(x) < 800 )
    		    		return false;
    		    	

    		    	// ============ retreat on building CD ========					
    				double dist = ((Building)x).getAttackRange() - SELF.getDistanceTo(x) + 20;
    				
    		    	if (getEnemyBuildingCD((Building)x) > 0) {
	    				if (x.getDistanceTo(SELF) <= ((Building)x).getAttackRange() + 10
	    						&& (int)(dist / wizardBackwardSpeed.get(SELF)) + 1 <= getEnemyBuildingCD((Building)x)
	    						&& buildingTarget == SELF
	    				)
	    					return true;
    		    	}

    				else {	// getEnemyBuildingCD((Building)x) == 0
	    				if ( 	(((Building)x).getType() == BuildingType.GUARDIAN_TOWER && lane == POWER_MAP.getPointLane(new Point2D(x))
	    							&& new Point2D(SELF).getDistanceTo(new Point2D(x)) < GAME.getGuardianTowerAttackRange() + wizardForwardSpeed.get(SELF) + 1)
	    					||	(((Building)x).getType() == BuildingType.FACTION_BASE
	        						&& new Point2D(SELF).getDistanceTo(new Point2D(x)) < GAME.getFactionBaseAttackRange() + wizardForwardSpeed.get(SELF) + 1)
	    				) {
							if (buildingTarget == SELF)
								return true;
							else
								return false;
	    				}
    				}
    				
    		    	
    				return getDistanceToUnitFromCenter(SELF, x) < selfRange * 0.95;

    			}
    			
    			
    			// ====================== WIZARDS ====================
    			
    			if (x instanceof Wizard) {
    				if (hasSkill((Wizard)x, SkillType.FROST_BOLT))
    					return getDistanceToUnitFromCenter(SELF, x) < selfRange * HOLD_DISTANCE_FACTOR;
    				
    				double enemyDPM = wizardMagicDamage.get((Wizard)x);
    				double myDPM = wizardMagicDamage.get(SELF);
    				double enemyLife = getWizardLife((Wizard)x);
    				double myLife = getWizardLife(SELF);
    				
    				if ((int)(enemyLife / myDPM) * 1.5 < (int)(myLife / enemyDPM))
    					return false;
    				
    				// ======== command play ==========
    				if (GAME.isRawMessagesEnabled()) {
    					if ( enemyWizardsCnt == 5 
    		    				&& (enemyWizardsByLane.get(lane).size() == 0
    		    					|| friendWizardsByLane.get(lane).size() / enemyWizardsByLane.get(lane).size() >= 5./3.)
    		    		) {
			    			boolean rush = SELF.getLife() > 70 ? true : false;
			    			/*for (Wizard w: friendWizardsByLane.get(lane)) {
			    				if (getWizardLife(w) > getWizardLife(SELF))
			    					rush = false;
			    			}*/
			    			if (rush)
			    				return false;
    					}
    				}
    			}
    			
    			
    			return getDistanceToUnitFromCenter(SELF, x) < selfRange * HOLD_DISTANCE_FACTOR;

    		})
    		.collect(Collectors.toList())
    		.size() > 0 ) {

    			isRetreat = true;
    			return;
    			
    	}
    	
		
    	// =========== hold distance enough close ==============
    	

    	LivingUnit u = getNearestEnemy();

		Point2D to = null;
		if (!isRetreat 
				&& enemyTarget != null
				&& u != null
				&& getDistanceToUnitFromCenter(SELF, u) > HOLD_DISTANCE_FACTOR_CONST) 
		{

			to = new Point2D(u);

			if (enemyTarget instanceof Building
					&& ((Building)enemyTarget).getType() == BuildingType.GUARDIAN_TOWER
					&& lane == POWER_MAP.getPointLane(new Point2D(enemyTarget))
					&& isUnitInStaffRange(enemyTarget)) {
				to = new Point2D(SELF);
			}
			
		} else {
			
			/*makeNearestPath(ENEMY_HOME_POINT, waypointsToMove);
			to = getNextWaypoint().point;*/
		}
    	
		targetPoint = to;

    }
    
    
    int getEnemyBuildingCD(Building b) {
		for (Building bb: enemyBuildingsCD.keySet())
			if (new Point2D(bb).getDistanceTo(new Point2D(b)) < 100) {
				return enemyBuildingsCD.get(bb);
			}
		
		return 0;
    }
    
    
    /**
     * 
     * @return
     */
    boolean canMoveToBonus() {

    	// move to get bonus at time it'll appears
    	double time = GAME.getBonusAppearanceIntervalTicks() - (WORLD.getTickIndex() % GAME.getBonusAppearanceIntervalTicks()) 
    			+ 70. / wizardForwardSpeed.get(SELF) - 60;
    	
    	makeNearestPath(new Waypoint(bonuses.get(0)), waypointsToBonus);
    	if (WORLD.getTickIndex() > 1500 && WORLD.getTickIndex() < 18000
    			&& lane != LaneType.BOTTOM && getTickCountToMove(SELF, waypointsToBonus) > time) {
    		bonusExist.put(bonuses.get(0), true);
    	}
    		
    	
    	makeNearestPath(new Waypoint(bonuses.get(1)), waypointsToBonus);
    	if (WORLD.getTickIndex() > 1500 && WORLD.getTickIndex() < 18000
    			&& lane != LaneType.TOP && getTickCountToMove(SELF, waypointsToBonus) > time) {
    		bonusExist.put(bonuses.get(1), true);
    	}
    	

    	// get nearest bonus
    	targetBonus = bonuses.stream()
    			.filter(x -> bonusExist.get(x).equals(true))	// bonus exist
    			.filter(x -> {									// I nearer to bonus then others 
    				if (GAME.isRawMessagesEnabled()) {
	    				for (Wizard w: friendWizards) 
	    					if (x.getDistanceTo(SELF) > x.getDistanceTo(w))
	    						return false;
    				}
    				
    				return true;
    			})
    			.min(new Comparator<Point2D>() {
					public int compare(Point2D o1, Point2D o2) {
						return o1.getDistanceToUnitCenter(SELF) - o2.getDistanceToUnitCenter(SELF) < 0 ? -1 : 1;
					}
    			})
    			.orElse((Point2D)null);
    	
    	if (targetBonus == null)
    		return false;
    	
    	
    	makeNearestPath(new Waypoint(targetBonus), waypointsToBonus);
    	
    	if ( targetBonus.getDistanceTo(SELF) > GAME.getWizardVisionRange() )
    		if (threatenedMeUnits.size() != 0 || !POWER_MAP.isWaySafe(waypointsToBonus) )
    			return false;
    	
    	if ( (!isDefeatLane || (isDefeatLane && targetBonus.getDistanceTo(SELF) < 1000))
    			&& (!isDefeat || (isDefeat && targetBonus.getDistanceTo(SELF) < 600) )
    			&& getTickCountToMove(SELF, waypointsToBonus) < CAN_MOVE_TO_BONUS_TICK_COUNT
    			)
    		return true;
    	
    	
    	return false;
    }

    
    /**
     * 
     * @param attacker
     * @param target
     * @return
     */
    boolean canShootTo(LivingUnit target, double radius) {
    	//List<CircularUnit> list = getUnitsInCorridor(new Point2D(SELF), new Point2D(target), radius, circularUnits);
    	return true;
    }
    
    
    double getDistanceToUnitFromCenter(CircularUnit centerUnit, CircularUnit radiusUnit) {
    	return new Point2D(centerUnit).getDistanceTo(radiusUnit);
    }
    
    
    
    boolean isUnitEnemy(LivingUnit unit) {
		if (unit.getFaction() == Faction.NEUTRAL || unit.getFaction() == SELF.getFaction()) {
			return false;
		} 
		return true;
	}
     
    
    
	boolean isUnitFriend(LivingUnit unit) {
		if (unit.getFaction() != SELF.getFaction()) {
			return false;
		} 
		return true;
	}
	
	
	
	boolean isUnitInCheckPowerArea(LivingUnit unit) {
		return /*getDistanceToUnitFromCenter(SELF, unit) < 800;*/ isUnitCanBeAttacked(SELF, unit);
	}
	
	
	
	boolean isUnitInCastRange(CircularUnit enemyTarget) {
		return inUnitRange(SELF, wizardCastRange.get(SELF), enemyTarget);
	}
	
	
	boolean inUnitRange(CircularUnit attacker, double range, CircularUnit attacked) {
		return attacker.getDistanceTo(attacked) - attacked.getRadius() <= range;
	}
	
	
	double getWizardCastRange(Wizard w) {
		List<Wizard> wizards;
		if (w.getFaction() == SELF.getFaction())
			wizards = friendWizards;
		else
			wizards = enemyWizards;
		
		
		double res = GAME.getWizardCastRange();
		
		// friend auras
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.RANGE_BONUS_AURA_1)) {
				res += GAME.getRangeBonusPerSkillLevel();
				break;
			}
		}
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.RANGE_BONUS_AURA_2)) {
				res += GAME.getRangeBonusPerSkillLevel();
				break;
			}
		}
		
		// self skills
		List<SkillType> skills = Arrays.asList(w.getSkills());
		if (skills.contains(SkillType.RANGE_BONUS_PASSIVE_1))
			res += GAME.getRangeBonusPerSkillLevel();
		if (skills.contains(SkillType.RANGE_BONUS_PASSIVE_2))
			res += GAME.getRangeBonusPerSkillLevel();
		
		
		return res;
	}
	
	double getWizardMagicDamage(Wizard w) {
		List<Wizard> wizards;
		if (w.getFaction() == SELF.getFaction())
			wizards = friendWizards;
		else
			wizards = enemyWizards;
		
		
		double res = GAME.getMagicMissileDirectDamage();
		
		// friend auras
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1)) {
				res += GAME.getMagicalDamageBonusPerSkillLevel();
				break;
			}
		}
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2)) {
				res += GAME.getMagicalDamageBonusPerSkillLevel();
				break;
			}
		}
		
		// self skills
		List<SkillType> skills = Arrays.asList(w.getSkills());
		if (skills.contains(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1)) {
			res += GAME.getMagicalDamageBonusPerSkillLevel();
		}
		if (skills.contains(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2)) {
			res += GAME.getMagicalDamageBonusPerSkillLevel();
		}
		
		for (Status s: w.getStatuses())
			if (s.getType() == StatusType.EMPOWERED)
				res *= GAME.getEmpoweredDamageFactor();
		
		return res;
	}
	
	double getWizardStaffDamage(Wizard w) {
		List<Wizard> wizards;
		if (w.getFaction() == SELF.getFaction())
			wizards = friendWizards;
		else
			wizards = enemyWizards;
		
		
		double res = GAME.getStaffDamage();
		
		// friend auras
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.STAFF_DAMAGE_BONUS_AURA_1)) {
				res += GAME.getStaffDamageBonusPerSkillLevel();
				break;
			}
		}
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.STAFF_DAMAGE_BONUS_AURA_2)) {
				res += GAME.getStaffDamageBonusPerSkillLevel();
				break;
			}
		}
		
		// self skills
		List<SkillType> skills = Arrays.asList(w.getSkills());
		if (skills.contains(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1)) {
			res += GAME.getStaffDamageBonusPerSkillLevel();
		}
		if (skills.contains(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2)) {
			res += GAME.getStaffDamageBonusPerSkillLevel();
		}
		
		for (Status s: w.getStatuses())
			if (s.getType() == StatusType.EMPOWERED)
				res *= GAME.getEmpoweredDamageFactor();
		
		return res;
	}
	
	boolean isUnitInCastSector(Wizard attacker, CircularUnit attacked) {
		return isUnitInSector(attacked, new Point2D(attacker), attacker.getAngle(), GAME.getStaffSector(), wizardCastRange.get(attacker));
	}
	
	boolean isUnitInStaffSector(Wizard attacker, CircularUnit attacked) {
		return isUnitInSector(attacked, new Point2D(attacker), attacker.getAngle(), GAME.getStaffSector(), GAME.getStaffRange());
	}
	
	
	boolean isUnitInSector(CircularUnit unit, Point2D point, double lookAngle, double sectorAngle, double dist) {
		if (point.getDistanceTo(unit) > dist)
			return false;
		
		Line l1 = new Line(point, normalizeAngle(lookAngle + sectorAngle/2));
		Line l2 = new Line(point, normalizeAngle(lookAngle - sectorAngle/2));
		
		// unit center in sector
		Point2D center = new Point2D(unit);

		if ( l1.getRelativePointPosition(center) + l1.getRelativePointPosition(l2.end) != 0
				&& l2.getRelativePointPosition(center) + l2.getRelativePointPosition(l1.end) != 0
				&& normalizeAngle(StrictMath.abs(SELF.getAngle() - new Line(new Point2D(SELF), new Point2D(unit)).angle)) < Math.PI / 2)
			return true;
		
		
		// intersect with sector
		List<Point2D> intersectList = l1.getCircleIntersection(new Circle(unit));
		intersectList.addAll(l2.getCircleIntersection(new Circle(unit)));
		if ( intersectList.stream()
				.filter(x -> x.getDistanceTo(point) <= dist)
				.filter(x -> normalizeAngle(StrictMath.abs(SELF.getAngle() - new Line(new Point2D(SELF), x).angle)) < Math.PI / 2)
				.collect(Collectors.toList()).size() > 0 )
			return true;
		
		
		return false;
	}
	
	
	boolean isUnitCanBeAttacked(LivingUnit attackedUnit, LivingUnit unit) {
		return inUnitRange(unit, getUnitRange(unit), attackedUnit);
	}
	
	
	double getWizardForwardSpeed(Wizard w) {
		List<Wizard> wizards;
		if (w.getFaction() == SELF.getFaction())
			wizards = friendWizards;
		else
			wizards = enemyWizards;
		
		
		double mul = 1;
		
		// friend auras
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1)) {
				mul += GAME.getMovementBonusFactorPerSkillLevel();
				break;
			}
		}
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2)) {
				mul += GAME.getMovementBonusFactorPerSkillLevel();
				break;
			}
		}
		
		// self skills
		List<SkillType> skills = Arrays.asList(w.getSkills());
		if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1)) {
			mul += GAME.getMovementBonusFactorPerSkillLevel();
		}
		if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2)) {
			mul += GAME.getMovementBonusFactorPerSkillLevel();
		}
		if (isWizardHastened(w))
			mul += GAME.getHastenedMovementBonusFactor();
		
		return mul * GAME.getWizardForwardSpeed();
	}
	
	
	double getWizardRotateSpeed(Wizard w) {
		double mul = 1;
		if (isWizardHastened(w))
			mul += GAME.getHastenedRotationBonusFactor();
		return mul * GAME.getWizardMaxTurnAngle();
	}
	
	
	double getWizardBackwardSpeed(Wizard w) {
		List<Wizard> wizards;
		if (w.getFaction() == SELF.getFaction())
			wizards = friendWizards;
		else
			wizards = enemyWizards;
		
		
		double mul = 1;
		
		// friend auras
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1)) {
				mul += GAME.getMovementBonusFactorPerSkillLevel();
				break;
			}
		}
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2)) {
				mul += GAME.getMovementBonusFactorPerSkillLevel();
				break;
			}
		}
		
		// self skills
		List<SkillType> skills = Arrays.asList(w.getSkills());
		if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1)) {
			mul += GAME.getMovementBonusFactorPerSkillLevel();
		}
		if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2)) {
			mul += GAME.getMovementBonusFactorPerSkillLevel();
		}
		if (isWizardHastened(w))
			mul += GAME.getHastenedMovementBonusFactor();
		
		return mul * GAME.getWizardBackwardSpeed();
	}
	
	
	double getWizardStrafeSpeed(Wizard w) {
		List<Wizard> wizards;
		if (w.getFaction() == SELF.getFaction())
			wizards = friendWizards;
		else
			wizards = enemyWizards;
		
		
		double mul = 1;
		
		// friend auras
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1)) {
				mul += GAME.getMovementBonusFactorPerSkillLevel();
				break;
			}
		}
		for (Wizard ww: wizards) {
			if (w.getDistanceTo(ww) > GAME.getAuraSkillRange())
				continue;
			
			List<SkillType> skills = Arrays.asList(ww.getSkills());
			if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2)) {
				mul += GAME.getMovementBonusFactorPerSkillLevel();
				break;
			}
		}
		
		// self skills
		List<SkillType> skills = Arrays.asList(w.getSkills());
		if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1)) {
			mul += GAME.getMovementBonusFactorPerSkillLevel();
		}
		if (skills.contains(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2)) {
			mul += GAME.getMovementBonusFactorPerSkillLevel();
		}
		if (isWizardHastened(w))
			mul += GAME.getHastenedMovementBonusFactor();
		
		return mul * GAME.getWizardStrafeSpeed();
	}
	
	
	double getWizardLife(Wizard w) {
		double life = w.getLife();
		if (isWizardShielded(w))
			life *= 1 + GAME.getShieldedDirectDamageAbsorptionFactor();
		return life;
	}
	
	
	boolean isWizardHastened(Wizard w) {
		List<Status> list = Arrays.asList(w.getStatuses());
		for (Status s: list)
			if (s.getType() == StatusType.HASTENED)
				return true;
		
		return false;
	}
	
	
	boolean isWizardEmpowered(Wizard w) {
		List<Status> list = Arrays.asList(w.getStatuses());
		for (Status s: list)
			if (s.getType() == StatusType.EMPOWERED)
				return true;
		
		return false;
	}
	
	
	boolean isWizardShielded(Wizard w) {
		List<Status> list = Arrays.asList(w.getStatuses());
		for (Status s: list)
			if (s.getType() == StatusType.SHIELDED)
				return true;
		
		return false;
	}
	
	
	boolean isWizardAdvancedMissile(Wizard w) {
		for (SkillType s: w.getSkills())
			if (s == SkillType.ADVANCED_MAGIC_MISSILE)
				return true;
		return false;
	}
	
	
	double getUnitRange(LivingUnit unit) {
		if (unit instanceof Minion) {
			double dist = 0;
			
			switch(((Minion)unit).getType()) {
				case ORC_WOODCUTTER: 
					dist = GAME.getOrcWoodcutterAttackRange();
					break;
				case FETISH_BLOWDART: 
					dist = GAME.getFetishBlowdartAttackRange();
					break;
			}
			
			return dist;
		}
		
		
		if (unit instanceof Wizard) {
			return wizardCastRange.get((Wizard)unit);
		}
		
		
		if (unit instanceof Building) {
			double dist = 0;
			
			switch(((Building)unit).getType()) {
				case FACTION_BASE: 
					dist = GAME.getFactionBaseAttackRange();
					break;
				case GUARDIAN_TOWER: 
					dist = GAME.getGuardianTowerAttackRange();
					break;
			}
			
			return dist;
		}
		
		
		return 0;
	}
	
	
	
	boolean isUnitInStaffRange(CircularUnit unit) {
		return inUnitRange(SELF, GAME.getStaffRange(), unit);
	}
    
    
    
    /**
     * move backward with save angle to enemyTarget
     */
    void retreat() {

        if (enemyTarget != null) {
        	targetTurn = new Point2D(enemyTarget.getX(), enemyTarget.getY());
        } else {
        	LivingUnit u = getNearestEnemy();

        	if (u == null || getDistanceToUnitFromCenter(SELF, u) + 20 < wizardCastRange.get(SELF))
        		targetTurn = getNextWaypoint().point;
        	else
        		targetTurn = new Point2D(u);
        }

        
        targetPoint = getRetreatWaypoint().point;
    }
    
    
    /**
     * 
     * @return
     */
    LivingUnit getNearestEnemy() {
    	return enemiesWithAggressiveMinions.stream().min(new Comparator<Object>() {
			public int compare(Object arg0, Object arg1) {
				return SELF.getDistanceTo((LivingUnit)arg0) - SELF.getDistanceTo((LivingUnit)arg1) < 0 ? -1 : 1; 
			}
    	}).orElse((LivingUnit)null);
    }
    
    
    /**
     * get angle in [-PI; PI]
     * @param angle
     * @return
     */
    double normalizeAngle(double angle) {
        if (angle > Math.PI)
        	angle -= Math.PI * 2;
        if (angle < -Math.PI)
        	angle += Math.PI * 2;
        
        return angle;
    }
    
    
    
    /**
     * 
     * @return
     */
    boolean isMyPowerBetter() {
        double enemyPower = enemyUnits.stream()
        		.filter(unit -> isUnitInCheckPowerArea(unit))
        		.mapToDouble(unit -> getUnitDPM(unit))
        		.sum();
        double friendPower = friendUnits.stream()
        		.filter(unit -> isUnitInCheckPowerArea(unit))
        		.mapToDouble(unit -> getUnitDPM(unit))
        		.sum();

        return (enemyPower <= friendPower);
    }
    

    
    /**
     * 
     * @param units
     * @return
     */
    LivingUnit getUnitWithLowestHP(List<LivingUnit> units) {
    	return units.stream()
    			.min(new Comparator<LivingUnit>() {
					public int compare(LivingUnit arg0, LivingUnit arg1) {
						return arg0.getLife() - arg1.getLife();
					}
				})
				.orElse(null);
    }
    
    
    
    /**
     * 
     * @return
     */
    List<LivingUnit> getLivingUnits() {
    	List<LivingUnit> units = new ArrayList<LivingUnit>();
        units.addAll(Arrays.asList(WORLD.getBuildings()));
        units.addAll(Arrays.asList(WORLD.getWizards()));
        units.addAll(Arrays.asList(WORLD.getMinions()));
        
        return units;
    }
    
    
    double getUnitDamage(Unit unit) {
    	double damage = 0;
    	
    	if (unit instanceof Wizard) {
    		double staffDamage = wizardStaffDamage.get((Wizard)unit);
    		double missileDamage = wizardMagicDamage.get((Wizard)unit);
    		if (isWizardEmpowered((Wizard)unit)) {
    			staffDamage *= GAME.getEmpoweredDamageFactor();
    			missileDamage *= GAME.getEmpoweredDamageFactor();
    		}
    		damage += staffDamage;
    		damage += missileDamage;
    	}
    	
    	else if (unit instanceof Building) {
    		Building building = (Building)unit;
    		damage = building.getDamage();
    	}
    	
    	else if (unit instanceof Minion) {
    		Minion minion = (Minion)unit;
    		damage = minion.getDamage();
    	}
    	
    	return damage;
    }
    
    
    double getUnitLife(LivingUnit u) {
    	if (u instanceof Wizard)
    		return getWizardLife((Wizard)u);
    	
    	return u.getLife();
    }
    
    
    /**
     * 
     * @param unit
     * @return unit	DPM
     */
    private double getUnitDPM(LivingUnit unit) {
    	double DPM = 0;
    	final int ticksUnit = 100;

    	if (unit instanceof Wizard) {
    		double staffDamage = wizardStaffDamage.get((Wizard)unit);
    		double missileDamage = wizardMagicDamage.get((Wizard)unit);
    		DPM += staffDamage * (ticksUnit / GAME.getStaffCooldownTicks());
    		//int missileCD = isWizardAdvancedMissile((Wizard)unit) ? GAME.getMagicMissileCooldownTicks() : GAME.getStaffCooldownTicks();
    		DPM += missileDamage * (ticksUnit / GAME.getMagicMissileCooldownTicks());
    		List<SkillType> skills = Arrays.asList(((Wizard)unit).getSkills());
    		if (skills.contains(SkillType.ADVANCED_MAGIC_MISSILE) || skills.contains(SkillType.FIREBALL))
    			DPM *= 1.5;
    	}
    	
    	else if (unit instanceof Building) {
    		Building building = (Building)unit;
    		DPM = building.getDamage() * (ticksUnit / building.getCooldownTicks());
    	}
    	
    	else if (unit instanceof Minion) {
    		Minion minion = (Minion)unit;
    		DPM = minion.getDamage() * (ticksUnit / minion.getCooldownTicks());
    	}
    	
    	
    	return DPM;
    }

    

    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
	private void initializeStrategy(Wizard self, Game game) {

        random = new Random(GAME.getRandomSeed());
        
        myLastPosition = new Point2D(SELF);
        stuckPoint = new Point2D(SELF);
        
        bonuses.add(new Point2D(1200, 1200));
        bonuses.add(new Point2D(2800, 2800));
        
        goToBonus = false;
        goToLane = true;
        
        enemyWizardsByLane.put(LaneType.TOP, new ArrayList<Wizard>());
        enemyWizardsByLane.put(LaneType.BOTTOM, new ArrayList<Wizard>());
        enemyWizardsByLane.put(LaneType.MIDDLE, new ArrayList<Wizard>());
        friendWizardsByLane.put(LaneType.TOP, new ArrayList<Wizard>());
        friendWizardsByLane.put(LaneType.BOTTOM, new ArrayList<Wizard>());
        friendWizardsByLane.put(LaneType.MIDDLE, new ArrayList<Wizard>());
        
        initWaypoints();
        
        initEnemiesBuildings();
        
        initSkills();
        
        FOREST = new Forest();
        
        POWER_MAP = new PowerMap();

        switch ((int) SELF.getId()) {
            /*case 6:
                lane = LaneType.TOP;
                break;*/
            case 4:
            case 5:
            case 9:
            case 10:
                lane = LaneType.BOTTOM;
                break;
            case 1:
            case 2:
            case 3:
            case 6:
            case 7:
            case 8:
            	lane = LaneType.MIDDLE;
        }
        
        // =========== command play ===========
        if (GAME.isRawMessagesEnabled()) {
	        if (SELF.getId() == 3 || SELF.getId() == 8)
	        	myRole = ROLE.HASTE;
        }
        
        //lane = LaneType.BOTTOM;

        restart();
        

        
    }
	
	
	
	void initSkills() {
		// Frost Bolt
		List<SkillType> frost = new ArrayList<SkillType>();
		frost.add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1);
		frost.add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1);
		frost.add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2);
		frost.add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
		frost.add(SkillType.FROST_BOLT);
		SKILLS_LISTING.put(SkillType.FROST_BOLT, frost);
		
		// magic missle CD
		List<SkillType> cd = new ArrayList<SkillType>();
		cd.add(SkillType.RANGE_BONUS_PASSIVE_1);
		cd.add(SkillType.RANGE_BONUS_AURA_1);
		cd.add(SkillType.RANGE_BONUS_PASSIVE_2);
		cd.add(SkillType.RANGE_BONUS_AURA_2);
		cd.add(SkillType.ADVANCED_MAGIC_MISSILE);
		SKILLS_LISTING.put(SkillType.ADVANCED_MAGIC_MISSILE, cd);
		
		// Fireball
		List<SkillType> fireball = new ArrayList<SkillType>();
		fireball.add(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1);
		fireball.add(SkillType.STAFF_DAMAGE_BONUS_AURA_1);
		fireball.add(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2);
		fireball.add(SkillType.STAFF_DAMAGE_BONUS_AURA_2);
		fireball.add(SkillType.FIREBALL);
		SKILLS_LISTING.put(SkillType.FIREBALL, fireball);
		
		// HASTE
		List<SkillType> haste = new ArrayList<SkillType>();
		haste.add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1);
		haste.add(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1);
		haste.add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2);
		haste.add(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2);
		haste.add(SkillType.HASTE);
		SKILLS_LISTING.put(SkillType.HASTE, haste);
		
		// SHILED
		List<SkillType> shield = new ArrayList<SkillType>();
		shield.add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1);
		shield.add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1);
		shield.add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2);
		shield.add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2);
		shield.add(SkillType.SHIELD);
		SKILLS_LISTING.put(SkillType.SHIELD, shield);
		
		// =============================================
		
		// RUSH
		Queue<SkillType> qRush = new LinkedList<SkillType>();
		qRush.addAll(SKILLS_LISTING.get(SkillType.FIREBALL));
		qRush.addAll(SKILLS_LISTING.get(SkillType.ADVANCED_MAGIC_MISSILE));
		qRush.addAll(SKILLS_LISTING.get(SkillType.HASTE));
		qRush.addAll(SKILLS_LISTING.get(SkillType.SHIELD));
		qRush.addAll(SKILLS_LISTING.get(SkillType.FROST_BOLT));
		
		roleSkills.put(ROLE.RUSH, qRush);
		
		
		// DEFEAT
		Queue<SkillType> qDefeat = new LinkedList<SkillType>();
		qDefeat.addAll(SKILLS_LISTING.get(SkillType.ADVANCED_MAGIC_MISSILE));
		qDefeat.addAll(SKILLS_LISTING.get(SkillType.FROST_BOLT));
		qDefeat.addAll(SKILLS_LISTING.get(SkillType.HASTE));
		qDefeat.addAll(SKILLS_LISTING.get(SkillType.SHIELD));
		qDefeat.addAll(SKILLS_LISTING.get(SkillType.FIREBALL));
		
		roleSkills.put(ROLE.DEFEAT, qDefeat);
		
		
		// HASTE
		Queue<SkillType> qHaste = new LinkedList<SkillType>();
		qHaste.addAll(SKILLS_LISTING.get(SkillType.HASTE));
		qHaste.addAll(SKILLS_LISTING.get(SkillType.ADVANCED_MAGIC_MISSILE));
		qHaste.addAll(SKILLS_LISTING.get(SkillType.FROST_BOLT));
		qHaste.addAll(SKILLS_LISTING.get(SkillType.SHIELD));
		qHaste.addAll(SKILLS_LISTING.get(SkillType.FIREBALL));
		
		roleSkills.put(ROLE.HASTE, qHaste);
	}
	
	
	 
	
	
	void initEnemiesBuildings() {
		enemiesBuildingsList.add(new Building(-1, 3097.386941332821, 1231.9023805485247, 0., 0., 0., 
				Faction.OTHER, GAME.getGuardianTowerRadius(), (int)GAME.getGuardianTowerLife(), (int)GAME.getGuardianTowerLife(), 
				new Status[]{}, BuildingType.GUARDIAN_TOWER, GAME.getGuardianTowerVisionRange(), 
				GAME.getGuardianTowerAttackRange(), GAME.getGuardianTowerDamage(), GAME.getGuardianTowerCooldownTicks(), 0));
		enemiesBuildingsList.add(new Building(-2, 1687.8740025771563, 50.0, 0., 0., 0., 
				Faction.OTHER, GAME.getGuardianTowerRadius(), (int)GAME.getGuardianTowerLife(), (int)GAME.getGuardianTowerLife(), 
				new Status[]{}, BuildingType.GUARDIAN_TOWER, GAME.getGuardianTowerVisionRange(), 
				GAME.getGuardianTowerAttackRange(), GAME.getGuardianTowerDamage(), GAME.getGuardianTowerCooldownTicks(), 0));
		enemiesBuildingsList.add(new Building(-3, 3950.0, 1306.7422221916638, 0., 0., 0., 
				Faction.OTHER, GAME.getGuardianTowerRadius(), (int)GAME.getGuardianTowerLife(), (int)GAME.getGuardianTowerLife(), 
				new Status[]{}, BuildingType.GUARDIAN_TOWER, GAME.getGuardianTowerVisionRange(), 
				GAME.getGuardianTowerAttackRange(), GAME.getGuardianTowerDamage(), GAME.getGuardianTowerCooldownTicks(), 0));
		enemiesBuildingsList.add(new Building(-4, 3600.0, 400.0, 0., 0., 0., 
				Faction.OTHER, GAME.getFactionBaseRadius(), (int)GAME.getFactionBaseLife(), (int)GAME.getFactionBaseLife(), 
				new Status[]{}, BuildingType.FACTION_BASE, GAME.getFactionBaseVisionRange(), 
				GAME.getFactionBaseAttackRange(), GAME.getFactionBaseDamage(), GAME.getFactionBaseCooldownTicks(), 0));
		enemiesBuildingsList.add(new Building(-5, 2629.3396796483976, 350.0, 0., 0., 0., 
				Faction.OTHER, GAME.getGuardianTowerRadius(), (int)GAME.getGuardianTowerLife(), (int)GAME.getGuardianTowerLife(), 
				new Status[]{}, BuildingType.GUARDIAN_TOWER, GAME.getGuardianTowerVisionRange(), 
				GAME.getGuardianTowerAttackRange(), GAME.getGuardianTowerDamage(), GAME.getGuardianTowerCooldownTicks(), 0));
		enemiesBuildingsList.add(new Building(-6, 3650.0, 2343.2513553373133, 0., 0., 0., 
				Faction.OTHER, GAME.getGuardianTowerRadius(), (int)GAME.getGuardianTowerLife(), (int)GAME.getGuardianTowerLife(), 
				new Status[]{}, BuildingType.GUARDIAN_TOWER, GAME.getGuardianTowerVisionRange(), 
				GAME.getGuardianTowerAttackRange(), GAME.getGuardianTowerDamage(), GAME.getGuardianTowerCooldownTicks(), 0));
		enemiesBuildingsList.add(new Building(-7, 2070.7106781186544, 1600.0, 0., 0., 0., 
				Faction.OTHER, GAME.getGuardianTowerRadius(), (int)GAME.getGuardianTowerLife(), (int)GAME.getGuardianTowerLife(), 
				new Status[]{}, BuildingType.GUARDIAN_TOWER, GAME.getGuardianTowerVisionRange(), 
				GAME.getGuardianTowerAttackRange(), GAME.getGuardianTowerDamage(), GAME.getGuardianTowerCooldownTicks(), 0));
		
		enemyBuildingsByLane.put(LaneType.TOP, 2);
		enemyBuildingsByLane.put(LaneType.BOTTOM, 2);
		enemyBuildingsByLane.put(LaneType.MIDDLE, 2);
	}
    
    
    
    /**
     * make initialization from HOME
     */
    void restart() {       
        bonusExist.put(bonuses.get(0), false);
        bonusExist.put(bonuses.get(1), false);
        
        moveToBonuses = false;
        isDefeat = false;
        isDefeatLane = false;
        goToBonus = false;
        goToLane = false;
        isRetreatInForest = isRetreatToForest = false;

    }
    
    
    void initEnemyBuildings() {
		List<Building> buildingOnMap = Arrays.asList(WORLD.getBuildings());
		
		outer:
		for (int i = 0; i < enemiesBuildingsList.size(); i++) {
			Building b = enemiesBuildingsList.get(i);
			
			// ========= if building is exist on map change it to new ==========

			for (Building j: buildingOnMap) {
				if (new Point2D(j).getDistanceTo(new Point2D(b)) < 10) {
					b = j;
					for (Building bb: enemyBuildingsCD.keySet())
						if (new Point2D(bb).getDistanceTo(new Point2D(b)) < 100)
							enemyBuildingsCD.put(bb, b.getRemainingActionCooldownTicks());
					continue outer;
				}
			}
    	    	

			// =========== if friends see point building doesn't exist ==========
			
			for (LivingUnit unit: friendUnits) {
	    		Circle c = getUnitVisionRange(unit);
    			
    			if (c.contains(new Point2D(b))) {
    				enemiesBuildingsList.remove(b);
    				
    				// remove from buildingsByLane
    				LaneType l;
    				if (b.getX() > 3500)
    					l = LaneType.BOTTOM;
    				else if (b.getY() < 500)
    					l = LaneType.TOP;
    				else
    					l = LaneType.MIDDLE;
    				
    				int n = enemyBuildingsByLane.get(l);
    				enemyBuildingsByLane.put(l, n-1);
    				
    				continue outer;
    			} 
    		}
			
			
			// ======= add building to enemies =======
			for (Building bb: enemyBuildingsCD.keySet())
				if (new Point2D(bb).getDistanceTo(new Point2D(b)) < 100) {
					int cd = enemyBuildingsCD.get(bb);
					enemyBuildingsCD.put(bb, Math.max(cd - 1, 0));
				}
			
			enemyUnits.add(b);

    	}
    }
    
    
    void initUnits() {
    	units = getLivingUnits();
        
        treeUnits = Arrays.asList(WORLD.getTrees());
        
        circularUnits = new ArrayList<CircularUnit>();
        circularUnits.addAll(units);
        circularUnits.removeIf(x -> SELF.getX() == x.getX() && SELF.getY() == x.getY());	// remove SELF
        circularUnits.addAll(treeUnits);
        
        circularUnitsInCheckObstacleRadius = circularUnits.stream()
				.filter(x -> new Point2D(SELF).getDistanceTo(new Point2D(x)) - x.getRadius() < CHECK_OBSTACACLES_RADIUS)
				.collect(Collectors.toList());
        
        
        friendUnits = units.stream()
        		.filter(unit -> isUnitFriend(unit))
        		.collect(Collectors.toList());
        
        friendUnitsExcludeMe = new ArrayList<LivingUnit>();
        friendUnitsExcludeMe.addAll(friendUnits);
        friendUnitsExcludeMe.removeIf(x -> SELF.getX() == x.getX() && SELF.getY() == x.getY());
        
        
        // neutrals minions
        List<LivingUnit> neutralsList = units.stream()
        		.filter(x -> x.getFaction() == Faction.NEUTRAL)
        		.collect(Collectors.toList());
        
        // update aggressive minions
        aggressiveMinions = new ArrayList<LivingUnit>();
        
        for (LivingUnit u: neutralsList) {
        	boolean isMinionExist = false;
        	
        	for (int i = 0; i < neutralsMinions.size(); i++) {
        		LivingUnit m = neutralsMinions.get(i);
        		
        		if (m.getId() == u.getId()) {
        			isMinionExist = true;
        			
        			if (m.getX() != u.getX() || m.getY() != u.getY() || ((Minion)u).getRemainingActionCooldownTicks() > 0) {
        				aggressiveMinions.add(u);
        			}
        		}
        	}
        	
        	if (!isMinionExist) {
        		neutralsMinions.add(u);
        	}
        }
        
        
        enemyUnits = units.stream()
        		.filter(unit -> isUnitEnemy(unit))
        		.collect(Collectors.toList());

        initEnemyBuildings();
        
        for (int i = 0; i < enemyUnits.size(); i++) {
        	LivingUnit b = enemyUnits.get(i);
        	
			if (new Point2D(b).getDistanceTo(new Point2D(3950, 1307)) < 10
					&& enemyBuildingsByLane.get(LaneType.BOTTOM) == 2)
				enemyUnits.remove(b);
			
			if (new Point2D(b).getDistanceTo(new Point2D(2630, 350)) < 10
					&& enemyBuildingsByLane.get(LaneType.TOP) == 2)
				enemyUnits.remove(b);
			
			if (new Point2D(b).getDistanceTo(new Point2D(3097, 1232)) < 10
					&& enemyBuildingsByLane.get(LaneType.MIDDLE) == 2)
				enemyUnits.remove(b);
        }
        
        enemiesWithAggressiveMinions = new ArrayList<LivingUnit>();
        enemiesWithAggressiveMinions.addAll(enemyUnits);
        enemiesWithAggressiveMinions.addAll(aggressiveMinions);
        
        
        // ================ friend wizards ==============
        
        friendWizards = friendUnits.stream()
      		   .filter(unit -> unit instanceof Wizard)
      		   .map(x -> (Wizard)x)
      		   .collect(Collectors.toList());
        
        for (Wizard w: friendWizards) {
        	if (w.getDistanceTo(SELF) < 10) 
        		SELF = w;
        }

        for (Wizard w: friendWizards) {
        	LaneType wizardLane = POWER_MAP.getPointLane(new Point2D(w));
        	
        	if (wizardLane == null)
        		continue;
        	
        	for (LaneType l: friendWizardsByLane.keySet()) {
        		List<Wizard> list = friendWizardsByLane.get(l);
        		for (int i = 0; i < list.size(); i++) {
        			Wizard ww = list.get(i);
        			if (ww.getId() == w.getId())
        				friendWizardsByLane.get(l).remove(ww);
        		}
        	}
        	friendWizardsByLane.get(wizardLane).add(w);
        }
        
        
        /*for (Wizard f: friendWizards) 
            for (Wizard w: wizardLane.keySet()) {
            	if (f.getId() == w.getId()) {
            		LaneType l = wizardLane.get(w);
            		wizardLane.remove(w);
            		wizardLane.put(f, l);
            		break;
            	}
            	wizardLane.put(f, null);
            }*/
  
        
        // ============= enemyWizards ===============
        
        List<Wizard> newWizardList = enemyUnits.stream()
      		   .filter(unit -> unit instanceof Wizard)
      		   .map(x -> (Wizard)x)
      		   .collect(Collectors.toList());


        // remove died
        for (int i = 0; i < enemyWizards.size(); i++) {
        	Wizard e = enemyWizards.get(i);
        	for (LivingUnit f: friendUnits) {
        		double speed = wizardForwardSpeed.get(e);
        		if (f.getDistanceTo(e) + speed + 4 < getUnitVisionRange(f).r) {
        			boolean exist = false;
        			for (Wizard n: newWizardList) 
        				if (n.getId() == e.getId()) {
        					exist = true;
        					break;
        				}
        			 
        			if (!exist) {
        				for (LaneType l: enemyWizardsByLane.keySet())
        					enemyWizardsByLane.get(l).remove(e);
    					enemyWizards.remove(e);
        			}
        		}
        	}
        }
        
        if (enemyWizards.size() > 5)
        	enemyWizards.clear();
        
        // get new Wizards
        //enemyWizards = newWizardList;
        for (Wizard n: newWizardList) {
        	boolean exist = false;
        	for (int i = 0; i < enemyWizards.size(); i++) {
            	Wizard w = enemyWizards.get(i);
            	
        		if (w.getId() == n.getId()) {
        			enemyWizards.remove(w);
        			enemyWizards.add(n);
        			exist = true;
        			break;
        		}
        	}
        	
        	if (!exist)
        		enemyWizards.add(n);
        }


        for (Wizard w: enemyWizards) {
        	for (LaneType l: enemyWizardsByLane.keySet()) {
        		List<Wizard> list = enemyWizardsByLane.get(l);
        		for (int i = 0; i < list.size(); i++) {
        			Wizard lw = list.get(i);
        			if (lw.getId() == w.getId()) {
        				enemyWizardsByLane.get(l).remove(lw);
        				enemyWizardsByLane.get(l).add(w);
        			}
        		}
        	}
        }
        
        for (Wizard w: enemyWizards) {
        	LaneType wizardLane = POWER_MAP.getPointLane(new Point2D(w));
        	
        	if (wizardLane == null)
        		continue;
        	
        	for (LaneType l: enemyWizardsByLane.keySet()) {
        		List<Wizard> list = enemyWizardsByLane.get(l);
        		for (int i = 0; i < list.size(); i++) {
        			Wizard lw = list.get(i);
        			if (lw.getId() == w.getId()) {
        				enemyWizardsByLane.get(l).remove(lw);
        			}
        		}
        	}
        	enemyWizardsByLane.get(wizardLane).add(w);
        }

    	// get count
        int newWizardCnt = 0;
        newWizardCnt += enemyWizardsByLane.get(LaneType.TOP).size();
        newWizardCnt += enemyWizardsByLane.get(LaneType.BOTTOM).size();
        newWizardCnt += enemyWizardsByLane.get(LaneType.MIDDLE).size();
        
    	enemyWizardsCnt = Math.max(enemyWizardsCnt, newWizardCnt);

        
        
        // ============= Wizards characteristic =================
        wizardForwardSpeed.clear();
        wizardBackwardSpeed.clear();
        wizardRotateSpeed.clear();
        wizardStrafeSpeed.clear();
        wizardCastRange.clear();
        wizardMagicDamage.clear();
        wizardStaffDamage.clear();
        
        for (Wizard w: friendWizards) {
        	wizardForwardSpeed.put(w, getWizardForwardSpeed(w));
        	wizardBackwardSpeed.put(w, getWizardBackwardSpeed(w));
        	wizardRotateSpeed.put(w, getWizardRotateSpeed(w));
        	wizardStrafeSpeed.put(w, getWizardStrafeSpeed(w));
        	wizardCastRange.put(w, getWizardCastRange(w));
        	wizardMagicDamage.put(w, getWizardMagicDamage(w));
        	wizardStaffDamage.put(w, getWizardStaffDamage(w));
        }
        for (Wizard w: enemyWizards) {
        	wizardForwardSpeed.put(w, getWizardForwardSpeed(w));
        	wizardBackwardSpeed.put(w, getWizardBackwardSpeed(w));
        	wizardRotateSpeed.put(w, getWizardRotateSpeed(w));
        	wizardStrafeSpeed.put(w, getWizardStrafeSpeed(w));
        	wizardCastRange.put(w, getWizardCastRange(w));
        	wizardMagicDamage.put(w, getWizardMagicDamage(w));
        	wizardStaffDamage.put(w, getWizardStaffDamage(w));
        }
        

        // ===========================================

        
        enemiesInCastRange = enemiesWithAggressiveMinions.stream()
    		   	.filter(unit -> isUnitInCastRange(unit))
        		.collect(Collectors.toList());
        
        enemiesInStaffRange = enemiesWithAggressiveMinions.stream()
    		   	.filter(unit -> isUnitInStaffRange(unit))
        		.collect(Collectors.toList());
       
        threatenedMeUnits = enemiesWithAggressiveMinions.stream()
    		   .filter(unit -> isUnitCanBeAttacked(SELF, unit))
    		   .collect(Collectors.toList());
        
        
    }

    
    void initBonuses() {
    	targetBonus = null;
    	
    	if (random != null) {

        	// if anyone see bonus
        	for (LivingUnit unit: friendUnits) {
        		Circle c = getUnitVisionRange(unit);
        		
        		Point2D b1 = bonuses.get(0),
        				b2 = bonuses.get(1);
        		
        		boolean isBonus1OnMap = false, 
        				isBonus2OnMap = false;
        		for (Bonus b: Arrays.asList(WORLD.getBonuses())) {
        			if (b.getX() == 1200) {
        				isBonus1OnMap = true;
        				/*isBonus2OnMap = false;
        				bonusExist.put(b2, false);*/
        			}
        			if (b.getX() == 2800) {
        				isBonus2OnMap = true;
        				/*isBonus1OnMap = false;
        				bonusExist.put(b1, false);*/
        			}
        		}
        		
        		
        		if (c.contains(b1) && isBonus1OnMap == false)
        			bonusExist.put(b1, false);
        		if (c.contains(b2) && isBonus2OnMap == false)
        			bonusExist.put(b2, false);
        	}
        	
        	// if bonus appear
	        if (WORLD.getTickIndex() > 1000 
	        		&& WORLD.getTickIndex() < 18000
	        		&& WORLD.getTickIndex() % GAME.getBonusAppearanceIntervalTicks() == 0) {
	        	bonusExist.put(bonuses.get(0), true);
	        	bonusExist.put(bonuses.get(1), true);
	        }
        }
    }
    
    
    
    LivingUnit getUnitById(long id) {
    	for (LivingUnit u: units)
    		if (u.getId() == id)
    			return u;
    	
    	return null;
    }
    
    
    
    Projectile checkProjectiles() {
    	for (Projectile p: projectiles) {

    		Point2D pStart = projectilesStart.get(p);
    		Line l = new Line(pStart, new Point2D(pStart.x + p.getSpeedX(), pStart.y + p.getSpeedY()));
    		
    		Circle c = new Circle(SELF);
    		c.r += p.getRadius() + 0.5;

    		
    		if (l.getCircleIntersection(c).size() == 0)
    			continue;
    		
    		if (p.getType() == ProjectileType.DART)
    			continue;
    		
    		
    		
    		double speed = StrictMath.hypot(p.getSpeedX(), p.getSpeedY());
    		double radius = p.getRadius();
    		LivingUnit owner = getUnitById(p.getOwnerUnitId());
    		double range = 0;
    		if (owner == null)
    			range = 600;
    		else
    			range = wizardCastRange.get((Wizard)owner);
    		double distToMe = pStart.getDistanceTo(SELF) - radius - 0.5;
    		
    		
    		if (range < distToMe)
    			continue;

    		
    		/* move back */
    		double dist = range + SELF.getRadius() + radius + 0.5;
    		double myDist = dist;
    		if (owner != null) 
    			myDist -= pStart.getDistanceTo(SELF) + SELF.getRadius();
    		else
    			myDist -= 600;
    		int myTicks = (int)(myDist / wizardBackwardSpeed.get(SELF)) + 1;
    		int ticks = (int)(range / speed) + 1;
    		if (WORLD.getTickIndex() >= 9832) {
    	    	System.out.println(ticks + " " + myTicks);
    	    }
    		if (myTicks <= ticks) {
    			moveBackward = true;
    			return p;
    		}
    		
    		/* move aside */
    		myTicks = (int)((SELF.getRadius() + radius + 0.1) / wizardForwardSpeed.get(SELF)) + 1;
    		ticks = (int)(distToMe / speed) + 1;

    		if (myTicks <= ticks)
    			return p;
    	}
    	
    	return null;
    }
    
    
    void initProjectiles() {
    	List<Projectile> list = Arrays.asList(WORLD.getProjectiles());
    	
    	for (Projectile p: list) {
    		// init wizards by lane by projectile
    		/*if (p.getFaction() != SELF.getFaction()) {
    			LaneType l = POWER_MAP.getRectLane(POWER_MAP.getPointRect(new Point2D(p)));
    			if (l != null) {
    				int id = (int)p.getOwnerUnitId();
    				Wizard target = null;
    				
    				for (LaneType lt: enemyWizardsByLane.keySet()) {
    					List<Wizard> wizardsList = enemyWizardsByLane.get(lt);
    					
    					for (int i = 0; i < wizardsList.size(); i++) {
    						Wizard w = wizardsList.get(i);
    						if (w.getId() == (long)id) {
    							target = w;
    							wizardsList.remove(w);
    							break;
    						}
    					}
    					
    				}
    				
    				if (target == null)
    					target = new Wizard((long)id,0.,0.,0.,500.,500.,Faction.OTHER,0.,0, 0, new Status[]{}, 0, false, 0, 0, 0.,0.,0, 0, new SkillType[]{}, 0, null, false, null); 
    				enemyWizardsByLane.get(l).add(target);
    			}
    		}*/
    		
    		
    		if (new Point2D(p).getDistanceTo(SELF) >= 700)
    			continue;
    		
    		boolean exist = false;
    		for (Projectile j: projectiles) {
    			if (j.getOwnerUnitId() == p.getOwnerUnitId()) {
    				j = p;
    				exist = true;
    				break;
    			}
    		}

    		// add new
    		if (!exist && p.getFaction() != SELF.getFaction()) {
    			projectiles.add(p);
    			Point2D start = new Point2D(p.getX() - p.getSpeedX(), p.getY() - p.getSpeedY());
    			
    			projectilesStart.put(p, start);
    		}
    	}
    	
    	
    	// delete projectiles
    	for (int i = 0; i < projectiles.size(); i++) {
    		Projectile p = projectiles.get(i);
    		boolean exist = false;
    		
    		for (Projectile j: list) {
    			if (j.getOwnerUnitId() == p.getOwnerUnitId()) {
    				exist = true;
    				break;
    			}
    		}
    		
    		if (!exist) {
    			projectiles.remove(p);
    			projectilesStart.remove(p);
    		}
    	}
    	
    }
    
    

    
    
    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     * (call this first)
     */
    private void initializeTick(Wizard self, World world, Game game, Move move) {
   
        
        enemyTarget = obstacleTarget = null;
        HOLD_DISTANCE_FACTOR = 0.75;
        targetPoint = targetTurn = null;
        actionType = null;
        
        isTurnRetreat = false;
        isRetreat = false;
        isWayDirect = false;
        
        pointToAvoidMagicAttack = null;
        avoidProjectile = null;
        moveBackward = false;
        
        
        initUnits();
        initBonuses();
        initProjectiles();
        
        
        if (SELF.getSkills().length >= 5 || SELF.getLife() > SELF.getMaxLife() * LOW_HP_FACTOR)
        	CAN_MOVE_TO_BONUS_TICK_COUNT = 400;
        else
        	CAN_MOVE_TO_BONUS_TICK_COUNT = 200;

        
        
        // ========= WAYPOINTS =============
        
        POWER_MAP.calculateMapPower();
        
        if (POWER_MAP.getMyRect() == null)
        	isRetreatToForest = false;
        
        if (goToLane && POWER_MAP.getRectLane(POWER_MAP.getMyRect()) == lane)
        	goToLane = false;
        
        if (!FOREST.contains(new Point2D(SELF)))
        	isRetreatInForest = false;
        
        
        // ========= command play ===========

        isCommandLaneRush = estimateRush();
        messageLane.clear();
        messageSkill.clear();
        messageRaw.clear();
        
    }
    
    
    void initSkillsQueue() {
    	int en = enemyWizardsByLane.get(lane).size(),
    		fr = friendWizardsByLane.get(lane).size();
/*
    	// find unfinished skill
    	SkillType unfinishedSkill = null;
    	List<SkillType> unfinishedSkillsList = SKILLS_LISTING.keySet().stream()
    			.filter(x -> {
    				int size = SKILLS_LISTING.get(x).size();
    				return size != 0 && size < 5;
    			}).collect(Collectors.toList());
    	if (unfinishedSkillsList.size() > 0)
    		unfinishedSkill = unfinishedSkillsList.get(0);


    	// =============
        SKILLS_QUEUE.clear();
        if (unfinishedSkill != null)
        	SKILLS_QUEUE.add(unfinishedSkill);

    	if ( fr < en
    			/*|| POWER_MAP.getRectsByLane(lane).indexOf(POWER_MAP.getNextByLane(lane)) 
    						<= POWER_MAP.getRectsByLane(lane).size() / 2 - 1*/
 /*   			|| (hasSkill(SELF, SkillType.ADVANCED_MAGIC_MISSILE) && !hasSkill(SELF, SkillType.FIREBALL)) 
    	) {
    		isSkillOrderSet = true;
    		SKILLS_QUEUE.add(SkillType.ADVANCED_MAGIC_MISSILE);
    		SKILLS_QUEUE.add(SkillType.FROST_BOLT);
    	} else {
    		SKILLS_QUEUE.add(SkillType.FIREBALL);
    		SKILLS_QUEUE.add(SkillType.ADVANCED_MAGIC_MISSILE);
    	}
    	SKILLS_QUEUE.add(SkillType.HASTE);
    	SKILLS_QUEUE.add(SkillType.SHIELD);
    	SKILLS_QUEUE.add(SkillType.FROST_BOLT);
    	
    	
    	SKILLS_TO_LEARN.clear();
    	for (SkillType s: SKILLS_QUEUE) 
    		SKILLS_TO_LEARN.addAll(SKILLS_LISTING.get(s));
    	
    	
    	// =============
    	if (SELF.getLevel() >= 5)
    		isSkillOrderSet = true;


    	if ( enemyWizardsCnt == 5 
    			|| enemyBuildingsByLane.get(lane) < 2 )
    		isSkillOrderSet = true;*/

    	if (myRole != null || WORLD.getTickIndex() < 600)
    		return;
    	
    	if (fr < en) {
    		myRole = ROLE.DEFEAT;
    		return;
    	}
    	
    	if (enemyWizardsCnt == 5
    			|| 5 - enemyWizardsCnt + en <= fr) {
    		myRole = ROLE.RUSH;
    		return;
    	}
    	
    	if (SELF.getLevel() >= 4)
    		myRole = ROLE.RUSH;
    }
    
    
    void checkStuck() {
    	if ( (targetBonus != null && targetBonus.getDistanceTo(SELF) < 100)
    			|| SELF.getRemainingActionCooldownTicks() != 0 )
    		return;
    	
    	
    	if (enemyTarget != null 
    			|| obstacleTarget != null
    			|| isRetreat ) {
        	stuckTick = 0;
        	return;
    	}
    	

		if (stuckTick == 0)
			stuckPoint = new Point2D(SELF);
		
		if (stuckPoint.getDistanceTo(new Point2D(SELF)) < 35)
			stuckTick++;
		else
			stuckTick = 0;
		
		if (stuckTick >= 50 && getWizardLife(SELF) > SELF.getMaxLife() * LOW_HP_FACTOR) {
			LivingUnit tree = (LivingUnit)Arrays.asList(WORLD.getTrees()).stream()
        			.min(new Comparator<Tree>() {
						public int compare(Tree o1, Tree o2) {
							return SELF.getDistanceTo(o1) - SELF.getDistanceTo(o2) < 0 ? -1 : 1;
						}
        			})
        			.orElse((Tree)null);
			obstacleTarget = tree;
			targetPoint = new Point2D(tree);
		}

    }
    
    
    Circle getUnitVisionRange(LivingUnit unit) {
    	Circle res = new Circle(unit);
    	double visionRange = 0;

    	if (unit instanceof Minion) {
			visionRange = ((Minion)unit).getVisionRange();
		}
		
		
		if (unit instanceof Wizard) {
			visionRange = ((Wizard)unit).getVisionRange();
		}
		
		
		if (unit instanceof Building) {
			switch(((Building)unit).getType()) {
				case FACTION_BASE: 
					visionRange = GAME.getFactionBaseVisionRange();
					break;
				case GUARDIAN_TOWER: 
					visionRange = GAME.getGuardianTowerVisionRange();
					break;
			}
		}
    	
    	res.r = visionRange;
    	return res;
    }
    
    
    void normalizeSpeed(Vector speed) {
    	double x = speed.x;
    	double y = speed.y;
    	
    	
    	if (x == 0) {
    		speed.y = y > 0 ? wizardStrafeSpeed.get(SELF) : -wizardStrafeSpeed.get(SELF);
    		return;
    	}
    	
    	
    	double k = y/x;
    	
    	if (x > 0) {
    		x = 1. / Math.sqrt( Math.pow(k/wizardStrafeSpeed.get(SELF), 2) + 1/Math.pow(wizardForwardSpeed.get(SELF), 2.) );
    	} else {
    		x = -1. / Math.sqrt( Math.pow(k/wizardStrafeSpeed.get(SELF), 2) + 1/Math.pow(wizardBackwardSpeed.get(SELF), 2.) );
    	}
    	
    	y = Math.abs(k * x);
    	y *= speed.y < 0 ? -1 : 1;
    	
    	speed.x = x;
    	speed.y = y;
    }
    
    
    Point2D handleObstacle(Line wayline, Point2D targetPoint) {

    	CircularUnit obstacle = findObstaclesOnWay(wayline);

        if (obstacle != null) {
        	List<Point2D> list = getPointsToAvoidObstacle(obstacle, targetPoint);
        	
        	
        	// ===== if there are free points then move to it ========
        	
        	List<Point2D> pointsCanBeIn = list.stream()		// can be at point 
        			.filter(x -> canMoveToPoint(x))
        			.collect(Collectors.toList());
        	
        	if (pointsCanBeIn.size() > 0) {
    			targetPoint = pointsCanBeIn.get(0);
        	}

        	
        	// =============== obstacle is Tree ================
        	
    		if (obstacle instanceof Tree) {
    			obstacleTarget = obstacle;
    		}
    		
    		if (pointsCanBeIn.size() > 0) {
    			return targetPoint;
    		}

    		// ============== if obstacle not Tree find nearest obstacles =========
    		
    		List<CircularUnit> nearestObstacles = new ArrayList<CircularUnit>();
    		nearestObstacles.add(obstacle);
    		
    		list.stream().filter(point -> {
    			// nearest points I can't move 
    			if (!canMoveToPoint(point)) {
    				// find ostacles contains this point
    				List<CircularUnit> u = circularUnitsInCheckObstacleRadius.stream()
            				.filter(unit -> {
            					Circle c = new Circle(unit);
            					c.r += SELF.getRadius() - 1;
            					return c.contains(point);
            				})
            				.collect(Collectors.toList());
    				
    				if (u.size() > 0) {
            			nearestObstacles.addAll(u);
    				} 
    			}
    			return false;
    		});
			
    		
    		// if there is Tree obstacle, attack it
			for (CircularUnit u: nearestObstacles) {
				if (u instanceof Tree) {
					obstacleTarget = u;
	    			return targetPoint;
				}
			}
			
			// if can't move and obstacle is neutral, attack him
			if (pointsCanBeIn.size() == 0) {
				for (CircularUnit u: nearestObstacles) {
					if (u instanceof LivingUnit && ((LivingUnit)u).getFaction() != SELF.getFaction()) {
						if (SELF.getLife() > 0.6 * SELF.getMaxLife())
							obstacleTarget = u;
		    			return targetPoint;
					}
				}
			}

        }
        
        return targetPoint;
    }
    
    
    /**
     * Простейший способ перемещения волшебника.
     */
    private void goTo(Point2D targetPoint) {
        Point2D mePoint = new Point2D(SELF);
        
    	if (targetPoint.x == mePoint.x && targetPoint.y == mePoint.y) {
    		MOVE.setSpeed(0);
    		MOVE.setStrafeSpeed(0);
    		return;
    	}

        Line wayline = new Line(mePoint, targetPoint);
        
        wayline = new Line(mePoint, handleObstacle(wayline, targetPoint));
 
        
        double wayAngle = wayline.angle;										// angle of wayline I retreat
        double angleToWay = normalizeAngle(-SELF.getAngle() + wayAngle);		// angle between me and wayline

        
        // calculate speed in two dimensions (for move with legal speed)
        
        double speed = 0, strafe = 0;

        double l = wayline.getLength();
        speed = l * StrictMath.cos(angleToWay);
        strafe = l * StrictMath.cos(-angleToWay + StrictMath.PI/2.);
        
        Vector v = new Vector(speed, strafe);
        normalizeSpeed(v);
        speed = v.x;
        strafe = v.y;
        
        
        if (goToBonus) {
        	double hyp = StrictMath.hypot(v.x, v.y);
        	double dist = targetBonus.getDistanceTo(new Point2D(SELF)) - SELF.getRadius() - GAME.getBonusRadius();
        	if (Arrays.asList(WORLD.getBonuses()).stream()
        			.filter(x -> x.getX() == targetBonus.getX() && x.getY() == targetBonus.getY())
        			.collect(Collectors.toList())
        			.size() == 0
        		&& dist < hyp + 1) {
        			
        			double k = (dist - 1) / hyp;
        			speed *= k;
        			strafe *= k;
        	}
        }

        
        MOVE.setSpeed(speed);
        MOVE.setStrafeSpeed(strafe);
        
    }
    
    
    boolean hasSkill(Wizard w, SkillType skill) {
    	SkillType s[] = w.getSkills();
    	for (SkillType a: s) 
    		if (a == skill)
    			return true;
    	
    	return false;
    }
    
    
    int getMagicCD(Wizard x) {
    	int cd = x.getRemainingCooldownTicksByAction()[2];
		if (hasSkill(x, SkillType.FROST_BOLT))
			cd = Math.min(cd, x.getRemainingCooldownTicksByAction()[3]);
		if (hasSkill(x, SkillType.FIREBALL))
			cd = Math.min(cd, x.getRemainingCooldownTicksByAction()[4]);
		
		return Math.max(cd, x.getRemainingActionCooldownTicks());
    }
    
    
    Point2D getPointToAvoidMagicAttack() {
    	
    	// ======= if enemies in staff range then dont move ========
    	if (enemiesInStaffRange.size() != 0 && !moveBackward) 
    		return null;
    	
    	
    	// ==============================
		Point2D start;
		
		
    	// ========== if there is projectile ===========
    	if (avoidProjectile != null) {
    		if (moveBackward)	
    			return new Point2D(SELF.getX() + avoidProjectile.getSpeedX() * 2, 
						  SELF.getY() + avoidProjectile.getSpeedY() * 2);
    		
    		start = projectilesStart.get(avoidProjectile);
    	}

    	else {
    		// ========= find wizard that can shoot soon =============
    		/*for (Wizard w: enemyWizards) {
    			
    		}*/
    		
    		
	    	// ======== if self cooldown < turn to 90deg then dont move ==========
			int selfcd = getMagicCD(SELF);
			
			if (selfcd <= (int)(Math.PI / 2 / wizardRotateSpeed.get(SELF)) + 1)
				return null;
			
	
			// ========== find wizards =========
			List<Wizard> list = enemyWizards.stream().filter(x -> {
				if (getMagicCD(x) > selfcd)
					return false;
				
				double dist = x.getDistanceTo(SELF);
				return (dist >= 400) && (dist <= 700);
			})
			.collect(Collectors.toList());
		
			
			if (list.size() == 0)
				return null;
		
			start = new Point2D(list.get(0));
    	}

    	
		// get point nearest to turn
		Line line = new Line(new Point2D(SELF), start);
		Line perp = line.getPerpendicular();
		
		Point2D end1 = perp.start.clone();
		Vector v = perp.direction.clone();
		double radius = GAME.getFireballRadius() + 1;
		if (avoidProjectile != null)
			radius = avoidProjectile.getRadius() + 1;
		v.normalize(SELF.getRadius() + radius);
		end1.moveOnVector(v);
		Line perp1 = new Line(perp.start, end1);
		
		Point2D end2 = perp.start.clone();
		end2.moveOnVector(perp1.direction.reverse());
		Line perp2 = new Line(perp.start, end2);
		
		double to1 = normalizeAngle(SELF.getAngle() - perp1.angle);
		double to2 = normalizeAngle(SELF.getAngle() - perp2.angle);

		boolean canMoveTo1 = canMoveToPoint(perp1.end);
		boolean canMoveTo2 = canMoveToPoint(perp2.end);
		
		if (canMoveTo1 && !canMoveTo2)
			return perp1.end;
		if (!canMoveTo1 && canMoveTo2)
			return perp2.end;
		if (Math.abs(to1) < Math.abs(to2))
			return perp1.end;
		else
			return perp2.end;
    		
    }
    
    
    void setTurnAfterMove(Point2D targetTurn) {
    	boolean done = false;

    	
    	if (isTurnRetreat) {
    		targetTurn = targetPoint;
    		done = true;
    	}
    	
    	
    	// ========= avoid magic ==============
    	if (!done && pointToAvoidMagicAttack != null) {
    		targetTurn = pointToAvoidMagicAttack;
    		done = true;
    	}
    	
    	
    	// ========= turn to enemy or waypoint ========
    	if (!done) {
	    	if (enemyTarget == null)
	    		enemyTarget = getNearestEnemy();
			if (enemyTarget != null && enemyTarget.getDistanceTo(SELF) <= 700)
				targetTurn = new Point2D(enemyTarget);
			else 
				targetTurn = getNextWaypoint().point;
    	}
    	
    	
    	double selfAngle = SELF.getAngle();
    	double speed = MOVE.getSpeed();
    	double strafe = MOVE.getStrafeSpeed();
        double speedX = speed * StrictMath.cos(selfAngle) + strafe * StrictMath.sin(selfAngle);
        double speedY = -speed * StrictMath.sin(selfAngle) + strafe * StrictMath.cos(selfAngle);

        MOVE.setTurn(normalizeAngle( 
        				new Line(new Point2D(SELF.getX() + speedX, SELF.getY() + speedY), targetTurn).angle - selfAngle
        			)
        );
    }
    
    
    List<CircularUnit> getUnitsInCorridor(Point2D start, Point2D end, double corridorRadius, List<CircularUnit> circularUnits) {
    	List<CircularUnit> res = new ArrayList<CircularUnit>();
    	
    	Segment segment = new Segment(start, end);
    	Line wayline = segment.line;
    	double dist = StrictMath.hypot(start.getDistanceTo(end), corridorRadius);
    	Line perpToWayline = wayline.getPerpendicular();
    	
    	circularUnits.stream().forEach(x -> {
    		if ( perpToWayline.getRelativePointPosition(end) + perpToWayline.getRelativePointPosition(new Point2D(x)) != 0
    				&& start.getDistanceTo(x) <= dist
    				&& new Point2D(x).getDistanceTo(wayline) < x.getRadius() + corridorRadius
    				&& segment.contains(new Point2D(x).getProjectionToLine(wayline)) )
    			res.add(x);
    	});

		
		return res;
    } 
    
    
    
    double getDistanceBetween(CircularUnit c1, CircularUnit c2) {
    	return c1.getDistanceTo(c2) - c1.getRadius() - c2.getRadius();
    }
    
    
    
    double getDistanceBetween(Circle c1, Circle c2) {
    	return c1.getDistanceTo(c2);
    }
    
    
    
    boolean canUnitBeAtPoint(CircularUnit unit, Point2D p) {
    	Circle c = new Circle(unit);
    	c.center = p;
    	
    	if (c.center.x - c.r <= 0 || c.center.x + c.r >= GAME.getMapSize()
    			|| c.center.y - c.r <= 0 || c.center.y + c.r >= GAME.getMapSize())
    		return false;
    	
    	for (CircularUnit u: circularUnitsInCheckObstacleRadius)
    		if (new Circle(u).getDistanceTo(c) < 0)
    			return false;
    	
    	return true;
    }
    
    
    boolean canMoveToPoint(Point2D point) {
    	if (point.x < SELF.getRadius() || point.x > 4000 - SELF.getRadius()
    			|| point.y < SELF.getRadius() || point.y > 4000 - SELF.getRadius())
    		return false;
    	return getUnitsInCorridor(new Point2D(SELF), point, SELF.getRadius(), circularUnitsInCheckObstacleRadius).size() == 0;
    }
    
    
    
    CircularUnit findObstaclesOnWay(Line wayline) {
    	
    	CircularUnit obstacle = getUnitsInCorridor(wayline.start, wayline.end, 
    			SELF.getRadius() + MOVE_AROUND_OBSTACACLE_RADIUS, circularUnitsInCheckObstacleRadius)
    			.stream()
        		.min(new Comparator<CircularUnit>() {
					public int compare(CircularUnit arg0, CircularUnit arg1) {
						return getDistanceBetween(SELF, arg0) - getDistanceBetween(SELF, arg1) > 0 ? 1 : -1;
					}
        		})
        		.orElse(null);

        return obstacle;
    }
    
    
    
    List<Point2D> getPointsToAvoidObstacle(CircularUnit obstacle, Point2D moveTo) {

    	List<Point2D> res = new ArrayList<Point2D>();
    	List<Point2D> possibleMovePoints = new ArrayList<Point2D>();
    	
    	// if stay close to obstacle then go perpendicular to it
    	if (getDistanceBetween(SELF, obstacle) < MOVE_AROUND_OBSTACACLE_RADIUS) {
    		Line l = new Line(new Point2D(SELF), new Point2D(obstacle));

    		l = l.getPerpendicular();
    		Vector v = l.direction.clone();
    		v.normalize(wizardStrafeSpeed.get(SELF));
    		
    		Point2D p = new Point2D(SELF);
    		p.moveOnVector(v);
    		possibleMovePoints.add(p);
    		
    		v.x *= -1; v.y *= -1;
    		Point2D p2 = new Point2D(SELF);
    		p2.moveOnVector(v);
    		possibleMovePoints.add(p2);
    	}
    	
    	else {
    		Circle c = new Circle(obstacle);
    		c.r += SELF.getRadius() + MOVE_AROUND_OBSTACACLE_RADIUS;
    		
    		possibleMovePoints.addAll(new Point2D(SELF).getTangentsToCircle(c)
    				.stream()
        			.map(x -> {
        				return new Point2D(obstacle).getProjectionToLine(x);
        			})
        			.collect(Collectors.toList())
        	);
    	}

		res = possibleMovePoints.stream()
	    	.sorted(new Comparator<Object>() {
				public int compare(Object arg0, Object arg1) {
					return ((Point2D)arg0).getDistanceTo(moveTo) - ((Point2D)arg1).getDistanceTo(moveTo) < 0 ? -1 : 1;
				}
	    	})
	    	.collect(Collectors.toList());
		
		return res;
    }

    
    /**
     * ===================== #Point2D ====================
     */
    class Point2D {
        private double x;
        private double y;

        private Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        private Point2D(Unit unit) {
            this.x = unit.getX();
            this.y = unit.getY();
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getDistanceTo(double x, double y) {
            return StrictMath.hypot(this.x - x, this.y - y);
        }

        public double getDistanceTo(Point2D point) {
            return getDistanceTo(point.x, point.y);
        }

        public double getDistanceToUnitCenter(Unit unit) {
            return getDistanceTo(unit.getX(), unit.getY());
        }
        
        public double getDistanceTo(CircularUnit unit) {
        	return getDistanceToUnitCenter(unit) - unit.getRadius();
        }
        
        
        
        Point2D getProjectionToLine(Line l) {
        	return l.getLineIntersection(l.getPerpendicular().moveToPoint(this));
        }
        
        double getDistanceTo(Line l) {
        	return StrictMath.abs(l.A * x + l.B * y + l.C) / StrictMath.sqrt(l.A*l.A + l.B*l.B);
        }
        
        
        
        List<Line> getTangentsToCircle(Circle circle) {
        	List<Line> res = new ArrayList<Line>();
        	

        	// stay close to circle return perpendicular
        	if (this.getDistanceTo(circle.center) < circle.r + 0.1) {
    			return res;
        	}
        	
        	
        	double y0 = circle.center.y;
        	double x0 = circle.center.x;
        	double k, d, f, a, b, c;
        	if ((x - x0) != 0) {
	        	k = -((y - y0) / (x - x0));
	        	d = -(x0*x0 - x*x + y0*y0 - y*y + Math.pow(this.getDistanceTo(circle.center), 2.)) / 2 / (x - x0);
	        	f = d - x0;
	        	a = k * k + 1;
	        	b = 2 * f * k - 2 * y0;
	        	c = f*f + y0*y0 - circle.r*circle.r;
        	} else if (y - y0 != 0) {
	        	k = -((x - x0) / (y - y0));
	        	d = -(x0*x0 - x*x + y0*y0 - y*y + Math.pow(this.getDistanceTo(circle.center), 2.)) / 2 / (y - y0);
	        	f = d - y0;
	        	a = k * k + 1;
	        	b = 2 * f * k - 2 * x0;
	        	c = f*f + x0*x0 - circle.r*circle.r;
        	} else {
        		return res;
        	}
        	
        	double D = b*b - 4*a*c;
        	
        	if (D == 0) {
        		double x, y;
        		if (this.x - x0 != 0)  {
	        		y = -b/2/a;
	        		x = k*y+d;
        		} else {
	        		x = -b/2/a;
	        		y = k*x+d;
        		}
        		res.add(new Line(this, new Point2D(x,y)));
        		
        	} else if (D > 0) {
        		double x1= 0, y1=0, x2=0, y2=0;
        		if (this.x - x0 != 0)  {
	        		y1 = (-b+StrictMath.sqrt(D))/2/a;
	        		x1 = k*y1+d;
	        		y2 = (-b-StrictMath.sqrt(D))/2/a;
	        		x2 = k*y2+d;
        		} else {
	        		x1 = (-b+StrictMath.sqrt(D))/2/a;
	        		y1 = k*x1+d;
	        		x2 = (-b-StrictMath.sqrt(D))/2/a;
	        		y2 = k*x2+d;
        		}
        		res.add(new Line(this, new Point2D(x1,y1)));
        		res.add(new Line(this, new Point2D(x2,y2)));
        	}
        	
        	return res;
        }
        
        
        void moveOnVector(Vector v) {
        	x += v.x;
        	y += v.y;
        }
        
        
        public String toString() {
        	return " x: " + x + " y " + y;
        }
        
        protected Point2D clone() {
        	return new Point2D(this.x, this.y);
        }
    }
    
    
    
    
    
    /**
     * ==================== #WAYPOINT ==================
     * @author user
     *
     */
    class Waypoint {
    	Point2D point;
    	Set<Waypoint> adjacent = new HashSet<Waypoint>();
    	Map<Waypoint, Double> distances = new HashMap<Waypoint, Double>();
    	
    	
    	Waypoint(double x, double y) {
    		point = new Point2D(x, y);
    	}

    	Waypoint(Point2D p) {
    		this(p.x, p.y);
    	}

    	void addAdjacent(Waypoint wp) {
    		adjacent.add(wp);
    	}
    	
    	double getDistanceTo(Waypoint p) {
			Segment way = new Segment(point, p.point);
    		double dist = way.getLength();

			double inForest = FOREST.getIntersectLength(way);

			return (dist - inForest) + inForest * 3;
    	}
    	
    	double getDistanceToAdjacent(Waypoint p) {
    		if (distances.containsKey(p))
    			return distances.get(p);
    		
    		return getDistanceTo(p);
    	}

    	
    	double getDistanceTo(LivingUnit unit) {
    		return point.getDistanceTo(new Point2D(unit.getX(), unit.getY()));
    	}

    }
    
    
    void calculateWaypointsDistance(Set<Waypoint> waypoints) {
    	for (Waypoint w: waypoints)
    		for (Waypoint a: w.adjacent)
    			if (a.distances.containsKey(w))
    				w.distances.put(a, a.distances.get(w));
    			else
    				w.distances.put(a, w.getDistanceTo(a));
    }

    
    Waypoint getWaypointByCoords(double x, double y) {
    	for (Waypoint wp: mapPoints)
    		if (wp.point.x == x && wp.point.y == y)
    			return wp;

    	return null;
    }
    

    /**
     * fill mapWaypoints, HOME_POINTS etc.
     * (call this only one time)
     */
    void initWaypoints() {
    	double mapSize = GAME.getMapSize();
    	
    	
    	// ========= INIT WAYPOINT ON MAP ==========
    	
    	Waypoint corner00 = new Waypoint(mapPad, mapPad);
    	Waypoint cornerX0 = new Waypoint(mapSize - 100, 100);
    	Waypoint corner0Y = new Waypoint(100, mapSize - 100);
    	Waypoint cornerXY = new Waypoint(mapSize - mapPad, mapSize - mapPad);
    	Waypoint center = new Waypoint(mapSize / 2, mapSize / 2);
    	
    	

		ENEMY_HOME_POINT = cornerX0;
		HOME_POINT = corner0Y;

		
		
		corner00.addAdjacent(cornerX0);
		corner00.addAdjacent(corner0Y);
		corner00.addAdjacent(center);
		
		cornerX0.addAdjacent(corner00);
		cornerX0.addAdjacent(cornerXY);
		cornerX0.addAdjacent(center);
		
		corner0Y.addAdjacent(corner00);
		corner0Y.addAdjacent(cornerXY);
		corner0Y.addAdjacent(center);
		
		cornerXY.addAdjacent(cornerX0);
		cornerXY.addAdjacent(corner0Y);
		cornerXY.addAdjacent(center);
		
		center.addAdjacent(cornerX0);
		center.addAdjacent(corner0Y);
		center.addAdjacent(corner00);
		center.addAdjacent(cornerXY);
		
		
		mapPoints.add(corner00);
		mapPoints.add(cornerX0);
		mapPoints.add(corner0Y);
		mapPoints.add(cornerXY);
		mapPoints.add(center);

    	
		targetWaypointByLane.put(LaneType.TOP, corner00);
		targetWaypointByLane.put(LaneType.BOTTOM, cornerXY);
		targetWaypointByLane.put(LaneType.MIDDLE, center);

    	
		waypoints = new HashSet<Waypoint>(mapPoints);
    }
    
    
    
    int getTickCountToMove(Wizard wizard, List<Waypoint> list) {
    	if (list.size() == 0) 
    		return 0;
    	
    	
    	double dist = new Waypoint(new Point2D(SELF)).getDistanceTo(list.get(0));    	
    	
    	for (int i = 1; i < list.size(); i++)
    		dist += list.get(i).getDistanceTo(list.get(i - 1));

    	return (int)(dist / wizardForwardSpeed.get(wizard));
    }

    
    
    /**
     * fill waypoints var with waypoints from me to target
     * @param target
     */
    void makeNearestPath(Waypoint target, List<Waypoint> list) {
    	list.clear();
    	
    	
    	/*if (isWayDirect) {
    		list.add(target);
    		return;
    	}*/
    	
    	
    	Waypoint self = new Waypoint(SELF.getX(), SELF.getY());
    	
    	Waypoint w = getWaypointByCoords(target.point.x, target.point.y);
    	boolean isPointNew = false;
    	if (w == null) {
    		isPointNew = true;
    	} else {
    		target = w;
    	}

    	
    	addWaypoint(self);
    	
    	
    	List<Waypoint> addictiveWaypoints = new ArrayList<Waypoint>();
    	if (FOREST.isIntersect(new Segment(self.point, target.point))) {
    		
    		Map<Waypoint, Boolean> used = new HashMap<Waypoint, Boolean>();
    		Queue<Waypoint> wps = new LinkedList<Waypoint>();
    		for (Waypoint wp: mapPoints)
    			used.put(wp, false);
    		wps.add(HOME_POINT);
    		while (!wps.isEmpty()) {
    			Waypoint wp = wps.poll();
    			used.put(wp, true);
    			
		    	for (Waypoint adj: wp.adjacent) {
		    		if (adj == self || adj == target) 
		    			continue;
		
		    		Waypoint n = new Waypoint(self.point.getProjectionToLine(new Line(wp.point, adj.point)));
		    		n.addAdjacent(wp);
		    		n.addAdjacent(adj);
		    		addictiveWaypoints.add(n);
		    		waypoints.add(n);
		    		
		    		if (!used.get(adj))
		    			wps.add(adj);
		    	}
    		}
    	}
	    	
    	for (Waypoint n: addictiveWaypoints) {
    		for (Waypoint adj: n.adjacent)
    			adj.addAdjacent(n);
    	}
    	
    	
    	if (isPointNew == true)
    		addWaypoint(target);
    	
    	
	    getNearestPath(self, target).stream()
	    	.forEach(x -> list.add(x));
    	
    	
    	removeWaypoint(self);
    	if (isPointNew)
    		removeWaypoint(target);
    	for (Waypoint n: addictiveWaypoints)
    		removeWaypoint(n);
    		
    }
    
    
    void removeWaypoint(Waypoint w) {
    	waypoints.remove(w);
    	
    	for (Waypoint p: waypoints) {
    		p.adjacent.remove(w);
    		p.distances.remove(w);
    	}
    }
    
    
    void addWaypoint(Waypoint w) {
    	for (Waypoint p: waypoints) {
    		w.addAdjacent(p);
    		p.addAdjacent(w);
    	}
	
    	waypoints.add(w);
    }
    
    
    
    void makePathToByLane(Waypoint to, List<Waypoint> way) {  
    	Waypoint targetWaypoint = targetWaypointByLane.get(lane);
    	if (targetWaypoint != null && targetWaypoint.getDistanceTo(to) 
    			< to.getDistanceTo(SELF) ) {
    		
    		makeNearestPath(targetWaypoint, way);
	    	getNearestPath(targetWaypoint, to).stream()
				.forEach(x -> way.add(x));
	    	
    	} else {
    		makeNearestPath(to, way);
    	}
    	
    }
    
    
    
    /**
     * 
     * @param start
     * @param target
     * @return list of waypoints from start to target without start
     */
    List<Waypoint> getNearestPath(Waypoint start, Waypoint target) {
    	Queue<Waypoint> queue = new LinkedList<Waypoint>();
    	
    	Map<Waypoint, Boolean> used = new HashMap<Waypoint, Boolean>();
    	waypoints.stream().forEach( x -> used.put(x, false));
    	
    	Map<Waypoint, Waypoint> parent = new HashMap<Waypoint, Waypoint>();
    	
    	Map<Waypoint, Double> distance = new HashMap<Waypoint, Double>();
    	double INF = Double.MAX_VALUE;
    	waypoints.stream().forEach( x -> distance.put(x, INF));

    	calculateWaypointsDistance(waypoints);
    	
    	queue.add(start);
    	used.put(start, true);
    	distance.put(start, 0.);
    	
    	
    	while (!queue.isEmpty()) {
    		Waypoint wp = queue.poll();
    		
    		for (Waypoint next: wp.adjacent) {
    			double dist = (double)distance.get(wp) + wp.getDistanceToAdjacent(next);
    			if (makeWaySafe) {
    				if (!POWER_MAP.isSegmentSafe(new Segment(wp.point, next.point), target.point)) {
    					dist *= 100000;
    				}
    			}
    			
    			if ((double)distance.get(next) > dist) {
    				distance.put(next, dist);
    				parent.put(next, wp);
    				used.put(next, false);
    			}
    			
    			if (!used.get(next)) {
    				queue.add(next);
    				used.put(next, true);
    			}
    		}
    	}
    	

    	// restore path
    	Waypoint wp = target;
    	List<Waypoint> res = new ArrayList<Waypoint>();
    	while (wp != start) {
    		res.add(wp);
    		wp = parent.get(wp);
    	}
    	
    	Collections.reverse(res);

    	return res;
    }
    
    
    /**
     * 
     * @return next waypoint on my way
     */
    private Waypoint getNextWaypoint() { 
    	if (isWayDirect)
    		return new Waypoint(targetPoint);
    	
    	
    	List<Waypoint> way;
    	if (goToBonus) {
    		way = waypointsToBonus;
    	} else {
    		way = waypointsToMove;
    	}
    	
    	
    	if (way.size() == 0)
    		return HOME_POINT;
    	
    	
    	for (Waypoint wp: way) {
    		if (wp.getDistanceTo(SELF) <= WAYPOINT_RADIUS) {
    			continue;
            }

    		return wp;
    	}
    	
    	return way.get(way.size() - 1);
    }
    
    
    List<PowerRect> getNearestRectsOnWay(List<PowerRect> list) {
    	List<PowerRect> possibleRects = new ArrayList<PowerRect>();
		List<PowerRect> myRects = new ArrayList<PowerRect>();
		for (PowerRect r: POWER_MAP.rects)
			if (r.contains(new Point2D(SELF))) {
				myRects.add(r);
			}
		
		// get PowerRects I can go from self point
		for (PowerRect r: list) {
			Point2D p = r.getCenter();
			
			// rects on way
			List<PowerRect> rects = POWER_MAP.getRectsOnWay(new Segment(new Point2D(SELF), p));
			
			// exclude rect I'm in
			rects.removeAll(myRects);
			
			// get nearest rect to me
			PowerRect rect = rects.stream().min(new Comparator<PowerRect>() {
							public int compare(PowerRect arg0, PowerRect arg1) {
								return arg0.getCenter().getDistanceTo(new Point2D(SELF)) - 
										arg1.getCenter().getDistanceTo(new Point2D(SELF)) > 0 ? 1 : -1;
							}
						})
						.orElse((PowerRect)null);
			
			if (rect != null)
				possibleRects.add(rect);
		}
		
		possibleRects.addAll(myRects);
    	
		return possibleRects;
    }
    
    
    Point2D getSafeRetreatPointOnRoad() {
    	List<PowerRect> rectsOnRoad = POWER_MAP.rects.stream()
    			.filter(x -> !(FOREST.isIntersect(new Segment(x.getCenter(), new Point2D(SELF)))))
				.collect(Collectors.toList());

    	List<PowerRect> possibleRects = getNearestRectsOnWay(rectsOnRoad);
    			
    	
    	// choose safest one
    	CircularUnit target = enemyTarget == null ? getNearestEnemy() : enemyTarget;
    	if (target != null) {
    		Line line = new Line(new Point2D(target), new Point2D(SELF)).getPerpendicular();
    		PowerRect r = possibleRects.stream()
    				.min(new Comparator<PowerRect>() {
						public int compare(PowerRect arg0, PowerRect arg1) {
							if (arg0.getEnemyPower() == arg1.getEnemyPower()) {
								
								if (POWER_MAP.getRectLane(arg0) != null && POWER_MAP.getRectLane(arg1) != null)
									return arg0.getCenter().getDistanceTo(HOME_POINT.point) < 
											arg1.getCenter().getDistanceTo(HOME_POINT.point) ? -1 : 1;
											
								double distToArg0 = arg0.getCenter().getDistanceTo(target)/* - arg0.getCenter().getDistanceTo(SELF)*/;
								double distToArg1 = arg1.getCenter().getDistanceTo(target)/* - arg1.getCenter().getDistanceTo(SELF)*/;
								boolean a1 = false, a0 = false;
								if (line.getRelativePointPosition(new Point2D(SELF)) + line.getRelativePointPosition(arg0.getCenter()) != 0)
									a0 = true;
								if (line.getRelativePointPosition(new Point2D(SELF)) + line.getRelativePointPosition(arg1.getCenter()) != 0)
									a1 = true;
								if (a0 && a1)
									return distToArg0 > distToArg1 ? -1 : 1;
								if (a1)
									return 1;
								if (a0)
									return -1;
								return distToArg0 > distToArg1 ? -1 : 1;
							}
							
							return arg0.getEnemyPower() < arg1.getEnemyPower() ? -1 : 1;
						}
					}).orElse((PowerRect)null);
    		
    		if (r != null)
    			return r.getCenter();
    		else 
    			return null;
    	}
    	
    	else {	// enemyTarget == null
    		return possibleRects.stream()
    				.min(new Comparator<PowerRect>() {
						public int compare(PowerRect arg0, PowerRect arg1) {
							if (arg0.getPowerOnRect() == arg1.getPowerOnRect()) {
								return arg0.getCenter().getDistanceTo(HOME_POINT.point) > 
										arg1.getCenter().getDistanceTo(HOME_POINT.point) ? 1 : -1;
							}
							
							return arg0.getPowerOnRect() < arg1.getPowerOnRect() ? -1 : 1;
						}
					}).get().getCenter();
    	}
    }
    
    
    Point2D getRetreatPointThroughForest() {
    	isRetreatToForest = true;
    	
    	PowerRect res = POWER_MAP.rects.stream()
    			.filter(x -> x.enemyPower == 0)
    			.filter(x -> FOREST.isIntersect(new Segment(x.getCenter(), new Point2D(SELF))))
    			.min(new Comparator<PowerRect>() {
    				public int compare(PowerRect r1, PowerRect r2) {
    					return r1.getCenter().getDistanceTo(SELF) > r2.getCenter().getDistanceTo(SELF) ? 1 : -1;
    				}
    			})
    			.orElse((PowerRect)null);
    		
    	if (res != null)
    		return res.getCenter();
    	
    	
    	List<Triangle> list = FOREST.parts.stream().sorted(new Comparator<Triangle>() {
    		public int compare(Triangle t1, Triangle t2) {
    			return t1.getCenter().getDistanceTo(SELF) > t2.getCenter().getDistanceTo(SELF) ? 1 : -1;
    		}
    	}).collect(Collectors.toList());
    	
    	if (list.get(0).getCenter().getDistanceTo(HOME_POINT.point) <= list.get(1).getCenter().getDistanceTo(HOME_POINT.point))
    		return list.get(0).getCenter();
    	else
    		return list.get(1).getCenter();
    }
    
    
    Point2D getRetreatPoint() {
    	if (FOREST.contains(new Point2D(SELF)))
    		isRetreatInForest = true;
    	
    	if (POWER_MAP.getRectLane(POWER_MAP.getMyRect()) != null) {
    		PowerRect r = POWER_MAP.getMyRect();
    		LaneType l = POWER_MAP.getRectLane(r);
    		if (l == LaneType.TOP) {
    			int i = POWER_MAP.topRects.indexOf(r);
    			if (i != 0 && POWER_MAP.topRects.get(i-1).enemyPowerInRect == 0)
    				return POWER_MAP.topRects.get(i-1).getCenter();
    			if (i == 0)
    				return HOME_POINT.point;
    			
    		} else if (l == LaneType.BOTTOM) {
    			int i = POWER_MAP.bottomRects.indexOf(r);
    			if (i != 0 && POWER_MAP.bottomRects.get(i-1).enemyPowerInRect == 0)
    				return POWER_MAP.bottomRects.get(i-1).getCenter();
    			if (i == 0)
    				return HOME_POINT.point;
    		}
    		
    		else {
    			int i = POWER_MAP.middleRects.indexOf(r);
    			
    			if (i != 0 && POWER_MAP.middleRects.get(i-1).enemyPowerInRect == 0)
    				return POWER_MAP.middleRects.get(i-1).getCenter();
    		}
    	}

    	Point2D roadPoint = getSafeRetreatPointOnRoad();
    	
    	PowerRect myRect = POWER_MAP.getMyRect();
    	if (isRetreatToForest
    			|| roadPoint == null 
    			|| myRect == null
    			|| (roadPoint.getDistanceTo(myRect.getCenter()) < 10 && myRect != POWER_MAP.middleRects.get(0)) ) {
    		return getRetreatPointThroughForest();
    	}
    	
    	
    	return roadPoint;

    }
    
    
    
    /**
     * 
     * @return
     */
	private Waypoint getRetreatWaypoint() {
		Waypoint retreatPoint = new Waypoint(getRetreatPoint());
		

		if (POWER_MAP.middleRects.get(0).contains(retreatPoint.point))
			retreatPoint = HOME_POINT;
		
		if (lane == LaneType.MIDDLE && POWER_MAP.middleRects.get(0).contains(new Point2D(SELF))) {
			Line l = new Line(new Point2D(0, 4000), new Point2D(4000, 0));
			if (l.getRelativePointPosition(new Point2D(SELF)) + l.getRelativePointPosition(new Point2D(0,0)) != 0)
				retreatPoint = new Waypoint(new Point2D(40, 3750));
			else
				retreatPoint = new Waypoint(new Point2D(250, 3960));
		}
		
		if (HOME_POINT.getDistanceTo(SELF) < 150) {
			if (POWER_MAP.topRects.get(0).enemyPowerInRect < POWER_MAP.bottomRects.get(0).enemyPowerInRect)
				retreatPoint = new Waypoint(POWER_MAP.topRects.get(0).getCenter());
			else
				retreatPoint = new Waypoint(POWER_MAP.bottomRects.get(0).getCenter());
		}

		
		isWayDirect = true;

    	return retreatPoint;
    }
	
	
	
	/**
	 * ===================== LINE ======================
	 */
	
	class Line {
		Point2D start = new Point2D(0,0), end = new Point2D(1,1);
		double A = 0, B = 0, C = 0, angle = 0;
		Vector direction = null;
		
		
		Line(Point2D s, Point2D e) {
			this.start = s;
			this.end = e;
			
			init();
			initDirection();
			calculateAngle();
		}
		
		
		Line(Point2D point, double angle) {
			double k = StrictMath.tan(angle);
			double dx = 0, dy = 10000;
			
			if (k == 0) {
				dy = 0;
				dx = 10000;
			} else {
				dx = dy / k;
			}
			
			
			if (k < 0) {
				dy *= -1; dx *= -1;
			}
			if (Math.abs(angle) > Math.PI / 2) {
				dx *= -1;
				dy *= -1;
			}
			
			
			this.start = point;
			this.end = new Point2D(point.x + dx, point.y + dy);
			
			init();
			initDirection();
			calculateAngle();
		}
		
		
		Line() {}
		
		private void init() {
			A = start.y - end.y;
			B = end.x - start.x;
			C = start.x * end.y - start.y * end.x;
		}
		
		void initDirection() {
			direction = new Vector(end.x - start.x, end.y - start.y);
		}
		
		
	    void calculateAngle() {
	    	double dx, dy;
	    	
	    	// line ordered
	    	if (direction != null) {
		    	dx = direction.x;
		    	dy = direction.y;
		    	
	    	} else {
	    		if (B == 0) {
	    			angle = 0;
	    			return;
	    		}
	    		
	    		double x1 = 0, x2 = 10;
	    		double y1 = -(C + x1 * A) / B;
	    		double y2 = -(C + x2 * A) / B;
	    		dx = x1 - x2;
	    		dy = y1 - y2;
	    	}
	        
	    	
	    	angle = StrictMath.atan(dy/dx);
	        
	        if (dy > 0) 
	        	angle = StrictMath.abs(angle);
	        else
	        	angle = -StrictMath.abs(angle);
	        
	        if (dx < 0)
	        	angle = -angle + Math.PI;

	        angle = normalizeAngle(angle);
	    }
	    
	    
	    double getLength() {
	    	return StrictMath.sqrt(direction.x * direction.x + direction.y * direction.y);
	    }
	    
	    
	    void moveOnVector(Vector v) {
	    	C = -( A * (start.x + v.x) + B * (start.y+v.y) );
	    }
	    
	    Line moveToPoint(Point2D p) {
	    	C = -( A * p.x + B * p.y );
	    	return this;
	    }
	    
	    double getRelativePointPosition(Point2D p) {
	    	return Math.signum(A*p.x+B*p.y+C);
	    }
	    
	    
	    Point2D getLineIntersection(Line l) {
	    	double denominator = this.A * l.B - l.A * this.B;
	    	
	    	if (denominator == 0)
	    		return null;
	    	
	    	return new Point2D((l.C * this.B - this.C * l.B)/denominator, (this.C * l.A - this.A * l.C)/denominator);
	    }
	    
	    
	    List<Point2D> getCircleIntersection(Circle circle) {
	    	List<Point2D> res = new ArrayList<Point2D>();
	    	
	    	double x0 = circle.center.x;
	    	double y0 = circle.center.y;
	    	double r = circle.r;
	    	
	    	double y1 = 0, y2 = 0, x1 = 0, x2 = 0;
    		double a = 0, b = 0, c = 0, d = 0;
    		
	    	if (A == 0) {
	    		y1 = y2 = -C/B;
	    		c = x0 * x0 + StrictMath.pow(y1-y0, 2.) - r*r;
	    		b = -2*x0;
	    		d = b*b-4*c*a;
	    		if (d==0) {
	    			x1 = -b/2;
	    		}
	    		if (d>0) {
	    			x1 = (-b+StrictMath.sqrt(d))/2;
	    			x2 = (-b-StrictMath.sqrt(d))/2;
	    		}
	    	} 
	    	
	    	else if (B == 0){
	    		x1 = x2 = -C/A;
	    		c = y0 * y0 + StrictMath.pow(x1-x0, 2.) - r*r;
	    		b = -2*y0;
	    		d = b*b-4*c*a;
	    		if (d==0) {
	    			y1 = -b/2;
	    		}
	    		if (d>0) {
	    			y1 = (-b+StrictMath.sqrt(d))/2;
	    			y2 = (-b-StrictMath.sqrt(d))/2;
	    		}
	    	}
	    	
	    	else {
	    		double k = B/A;
	    		double f = C/A + x0;
	    		a = k*k + 1;
	    		b = 2*k*f-2*y0;
	    		c = f*f+y0*y0-r*r;
	    		d = b*b-4*c*a;
	    		if (d==0) {
	    			y1 = -b/2/a;
	    			x1 = -(B*y1+C)/A;
	    		}
	    		if (d>0) {
	    			y1 = (-b+StrictMath.sqrt(d))/2/a;
	    			y2 = (-b-StrictMath.sqrt(d))/2/a;
	    			x1 = -(B*y1+C)/A;
	    			x2 = -(B*y2+C)/A;
	    		}
	    	}
	    	
    		if (d<0) return res;
    		if (d==0) {
    			res.add(new Point2D(x1, y1));
    			return res;
    		}
    		res.add(new Point2D(x1, y1));
    		res.add(new Point2D(x2, y2));
    		return res;
	    }
	    
	    
	    Line getReverse() {
	    	return new Line(end, start);
	    }
	    
	    
	    /**
	     * throught start point
	     * @return
	     */
	    Line getPerpendicular() {
	    	/*Line res = (Line) this.clone();
			res.direction = new Vector(direction.y, -direction.x);
	    	
	    	if (A == 0) {
	    		res.B = -A;
	    		res.A = B;
	    	}
	    	else {
	    		res.A = -B;
	    		res.B = A;
	    	}
	    	
	    	res.C = -(res.A * start.x + res.B * start.y);
	    	res.calculateAngle();
	    	
	    	return res;*/
	    	
	    	Point2D end = start.clone();
	    	end.moveOnVector(new Vector(direction.y, -direction.x));
	    	return new Line(start, end);
	    }
	    
	    
	    protected Line clone() {
	    	Line res = new Line();
	    	if (res.start != null) res.start = start.clone();
	    	if (res.end != null) res.end = end.clone();
	    	res.A = A;
	    	res.B = B;
	    	res.C = C;
	    	res.angle = angle;
	    	if (res.direction != null) res.direction = direction.clone();
	    	return res;
	    }

	    
	    public String toString() {
	    	return " start " + start.toString() + " | end " + end.toString() + " | angle " + (angle*180/Math.PI);
	    }
	}
	
	
	
	/**
	 * ================== CIRCLE =================
	 */
	class Circle {
		Point2D center;
		double r;
		
		public Circle(Point2D c, double r) {
			center = c;
			this.r = r;
		}
		
		Circle(CircularUnit unit) {
			this(new Point2D(unit), unit.getRadius());
		}
		
		boolean contains(Point2D p) {
			return StrictMath.pow((p.x-center.x), 2.) + StrictMath.pow((p.y-center.y), 2.) <= r * r;
		}
		
		double getDistanceTo(Circle c) {
			return StrictMath.hypot(center.x - c.center.x, center.y - c.center.y) - r - c.r;
		}
		
		public String toString() {
			return "center " + center.toString() + " r = " + r;
		}

	}
	
	
	List<Line> getCirclesCommonTangents(Circle c1, Circle c2) {
		List<Line> res = new ArrayList<Line>();
		double EPS = 1E-9;
		
		if (c1.getDistanceTo(c2) < EPS) {
			Line l = new Line(c1.center, c2.center);
			Point2D inter = c1.center.clone();
			
			Vector v = l.direction;
			v.normalize(c1.r);
			inter.moveOnVector(v);
			
			Line p = l.getPerpendicular();
			p.moveToPoint(inter);
			res.add(p);
			
			return res;
		}

		
		for (int i=-1; i<=1; i+=2)
			for (int j=-1; j<=1; j+=2) {
				Point2D c = new Point2D(c2.center.x-c1.center.x, c2.center.y-c1.center.y);
				double r1 = c1.r * i;
				double r2 = c2.r * j;
				double r = r2 - r1;
				double z = c.x * c.x + c.y * c.y;
				double d = z - r * r;
				if (d < -EPS)  continue;
				d = StrictMath.sqrt(StrictMath.abs(d));
				Line l = new Line();
				l.A = (c.x * r + c.y * d) / z;
				l.B = (c.y * r - c.x * d) / z;
				l.C = r1;
				l.calculateAngle();
				res.add(l);
			}
		
		for (int i=0; i<res.size(); ++i)
			res.get(i).C -= res.get(i).A * c1.center.x + res.get(i).B * c1.center.y;

		return res;
	}
	
	
	
	
	/**
	 * ================== VECTOR =================
	 * 
	 */
	class Vector {
		double x, y;
		
		Vector(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		Vector(Point2D s, Point2D e) {
			this(e.x - s.x, e.y - s.y);
		}
		
		protected Vector clone() {
			return new Vector(x, y);
		}
		
		Vector reverse() {
			Vector res = this.clone();
			res.x = -x;
			res.y = -y;
			return res;
		}
		
		void normalize(double length) {
			if (x == 0) {
				y = length * Math.signum(y);
				return;
			} 
			
			if (y == 0) {
				x = length * Math.signum(x);
				return;
			}
			
			double k = y/x;
			x = Math.signum(x) * length / StrictMath.sqrt(k*k+1);
			y = k * x;
			
		}
	}
	
	
	
	/**
	 * ====================== SEGMENT ==================
	 */
	class Segment {
		Line line;
		
		Segment(Point2D start, Point2D end) {
			line = new Line(start, end);
		}
		
		Point2D getIntersectionWithSegment(Segment s) {
			Point2D res = s.line.getLineIntersection(line);
			
			if (res == null)
				return res;
			
			double maxX = Math.max(line.end.x, line.start.x);
			double minX = Math.min(line.end.x, line.start.x);
			double maxY = Math.max(line.end.y, line.start.y);
			double minY = Math.min(line.end.y, line.start.y);
			if (res.x > maxX || res.x < minX || res.y > maxY || res.y < minY)
				return null;
			
			maxX = Math.max(s.line.end.x, s.line.start.x);
			minX = Math.min(s.line.end.x, s.line.start.x);
			maxY = Math.max(s.line.end.y, s.line.start.y);
			minY = Math.min(s.line.end.y, s.line.start.y);
			if (res.x > maxX || res.x < minX || res.y > maxY || res.y < minY)
				return null;
						
			return res;
		}
		
		double getLength() {
			return StrictMath.hypot(line.end.x - line.start.x, line.end.y - line.start.y);
		}
		
		boolean contains(Point2D p) {
			double minX = Math.min(line.start.x, line.end.x);
			double maxX = Math.max(line.start.x, line.end.x);
			double minY = Math.min(line.start.y, line.end.y);
			double maxY = Math.max(line.start.y, line.end.y);
			
			if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) 
				return false;
			
			if (p.getDistanceTo(line) > 0.1) 
				return false;
			
			return true;
		}
	}
	
	
	
	/**
	 * ================== TRIANGLE ====================
	 */
	class Triangle {
		Point2D p1, p2, p3;
		Segment s12, s13, s23;
		
		Triangle(Point2D p1, Point2D p2, Point2D p3) {
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			
			s12 = new Segment(p1, p2);
			s13 = new Segment(p1, p3);
			s23 = new Segment(p2, p3);
		}
		
		double getIntersectionLength(Segment s) {
			double res = 0;
			
			List<Point2D> list = new ArrayList<Point2D>();
			
			Point2D p1 = s12.getIntersectionWithSegment(s);
			Point2D p2 = s13.getIntersectionWithSegment(s);
			Point2D p3 = s23.getIntersectionWithSegment(s);
			if (p1 != null) list.add(p1);
			if (p2 != null) list.add(p2);
			if (p3 != null) list.add(p3);
			
			
			if (list.size() == 2) {
				res = list.get(0).getDistanceTo(list.get(1));
			}
			
			else if (list.size() == 1) {
				if (this.contains(s.line.start)) {
					res = list.get(0).getDistanceTo(s.line.start);
				} else if (this.contains(s.line.end)) {
					res = list.get(0).getDistanceTo(s.line.end);
				}
			}

			
			return res;
		}
		
		Point2D getCenter() {
			return new Point2D( (p1.x+p2.x+p3.x)/3, (p1.y+p2.y+p3.y)/3 );
		}
		
		boolean contains(Point2D p) {
			if (s12.line.getRelativePointPosition(p3) + s12.line.getRelativePointPosition(p) == 0)
				return false;
			if (s13.line.getRelativePointPosition(p2) + s13.line.getRelativePointPosition(p) == 0)
				return false;
			if (s23.line.getRelativePointPosition(p1) + s23.line.getRelativePointPosition(p) == 0)
				return false;
			
			return true;
		}
		
		boolean isIntersect(Segment s) {
			if (s12.getIntersectionWithSegment(s) != null)
				return true;
			if (s13.getIntersectionWithSegment(s) != null)
				return true;
			if (s23.getIntersectionWithSegment(s) != null)
				return true;
			
			return false;
		}
	}
	
	
	
	/**
	 * ================== FOREST ======================
	 */
	class Forest {
		List<Triangle> parts = new ArrayList<Triangle>();
		
		Forest(){
			init();
		}
		
		void init() {
	    	Point2D p11 = new Point2D(400, 800);
			Point2D p12 = new Point2D(400, 3200);
			Point2D p13 = new Point2D(1600, 2000);
			parts.add(new Triangle(p11,p12,p13));
			
			Point2D p21 = new Point2D(800, 400);
			Point2D p22 = new Point2D(3200, 400);
			Point2D p23 = new Point2D(2000, 1600);
			parts.add(new Triangle(p21,p22,p23));
			
			Point2D p31 = new Point2D(3600, 800);
			Point2D p32 = new Point2D(3600, 3200);
			Point2D p33 = new Point2D(2400, 2000);
			parts.add(new Triangle(p31,p32,p33));
			
			Point2D p41 = new Point2D(800, 3600);
			Point2D p42 = new Point2D(3200, 3600);
			Point2D p43 = new Point2D(2000, 2400);
			parts.add(new Triangle(p41,p42,p43));

	    }
		
		boolean isIntersect(Segment s) {
			for (Triangle t: parts)
				if (t.isIntersect(s))
					return true;
			
			return false;
		}
		
		/**
		 * use only for enemy target == base to hold right side
		 * @param point
		 * @return
		 */
		Point2D getDistanceToForest(Point2D point) {
			double dist = 10000000;
			Point2D res = null;
			
			for (Triangle r: new Triangle[]{parts.get(0), parts.get(2)})
				for (Segment s: new Segment[]{r.s12, r.s13, r.s23}) { 
					double d = point.getDistanceTo(s.line);
					if (d < dist) {
						Point2D p = point.getProjectionToLine(s.line);
						if (s.contains(p)) {
							dist = d;
							res = p;
						}
					}
				}

			return res;
		}
		
		double getIntersectLength(Segment s) {
			double res = 0;
			
			for (Triangle t: parts)
				res += t.getIntersectionLength(s);
			
			return res;
		}
		
		boolean contains(Point2D p) {
			for (Triangle t: parts)
				if (t.contains(p))
					return true;
			
			return false;
		}
	}
	
	
	/**
	 * ================= RECTANGLE ======================
	 */
	class Rectangle {
		Point2D p1, p2, p3, p4;
		Segment s1, s2, s3, s4;
		
		Rectangle(Point2D p1, Point2D p2, Point2D p3, Point2D p4) {
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			this.p4 = p4;
			s1 = new Segment(p1, p2);
			s2 = new Segment(p2, p3);
			s3 = new Segment(p3, p4);
			s4 = new Segment(p1, p4);
		}
		
		boolean isIntersect(Segment s) {
			if (this.contains(s.line.start) || this.contains(s.line.end))
				return true;
			if (s1.getIntersectionWithSegment(s) != null)
				return true;
			if (s2.getIntersectionWithSegment(s) != null)
				return true;
			if (s3.getIntersectionWithSegment(s) != null)
				return true;
			if (s4.getIntersectionWithSegment(s) != null)
				return true;
			return false;
		}
		
		boolean contains(Point2D p) {
			if (s1.line.getRelativePointPosition(p) + s1.line.getRelativePointPosition(p3) == 0)
				return false;
			if (s2.line.getRelativePointPosition(p) + s2.line.getRelativePointPosition(p1) == 0)
				return false;
			if (s3.line.getRelativePointPosition(p) + s3.line.getRelativePointPosition(p1) == 0)
				return false;
			if (s4.line.getRelativePointPosition(p) + s4.line.getRelativePointPosition(p3) == 0)
				return false;
			return true;
		}
		
		boolean contains(Unit u) {
			return contains(new Point2D(u));
		}
		
		Point2D getCenter() {
			if (this.contains(HOME_POINT.point))
				return HOME_POINT.point;
			
			Point2D res =  new Point2D(0,0);
			res.x = (p1.x + p2.x + p3.x + p4.x)/4;
			res.y = (p1.y + p2.y + p3.y + p4.y)/4;
			return res;
		}
	}
	
	
	/**
	 * ================== POWERMAP ====================
	 */
	class PowerMap {
		List<PowerRect> rects = new ArrayList<PowerRect>();
		List<PowerRect> middleRects = new ArrayList<PowerRect>();
		List<PowerRect> bottomRects = new ArrayList<PowerRect>();
		List<PowerRect> topRects = new ArrayList<PowerRect>();
		List<PowerRect> neutralRects = new ArrayList<PowerRect>();
		
		PowerMap() {
			init();
		}
		
		void init() {

			int N = 3;
			
			// left
			Point2D p2 = new Point2D(0, 800);
			Point2D p3 = new Point2D(400, 800);
			Point2D p4 = new Point2D(400, 3200);
			Point2D p1 = new Point2D(0, 3200);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				topRects.add(top);
			}
			// top
			p1 = new Point2D(800, 0);
			p2 = new Point2D(3200, 0);
			p3 = new Point2D(3200, 400);
			p4 = new Point2D(800, 400);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				topRects.add(top);
			}
			// bottom
			p1 = new Point2D(800, 3600);
			p2 = new Point2D(3200, 3600);
			p3 = new Point2D(3200, 4000);
			p4 = new Point2D(800, 4000);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				bottomRects.add(top);
			}
			
			// right
			p2 = new Point2D(3600, 800);
			p3 = new Point2D(4000, 800);
			p4 = new Point2D(4000, 3200);
			p1 = new Point2D(3600, 3200);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				bottomRects.add(top);
			}
			// 00 - center
			p2 = new Point2D(2000, 1600);
			p3 = new Point2D(1600, 2000);
			p4 = new Point2D(400, 800);
			p1 = new Point2D(800, 400);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				neutralRects.add(top);
			}
			// X0 - center
			p2 = new Point2D(2000, 1600);
			p3 = new Point2D(2400, 2000);
			p4 = new Point2D(3600, 800);
			p1 = new Point2D(3200, 400);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				middleRects.add(top);
			}
			// 0Y - center
			p2 = new Point2D(1600, 2000);
			p3 = new Point2D(2000, 2400);
			p4 = new Point2D(800, 3600);
			p1 = new Point2D(400, 3200);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				middleRects.add(top);
			}
			// XY - center
			p2 = new Point2D(3600, 3200);
			p3 = new Point2D(3200, 3600);
			p4 = new Point2D(2000, 2400);
			p1 = new Point2D(2400, 2000);
			p2.x = p1.x + (p2.x - p1.x) / N; p2.y = p1.y + (p2.y - p1.y) / N;
			p3.x = p4.x + (p3.x - p4.x) / N; p3.y = p4.y + (p3.y - p4.y) / N;
			for (int i = 0; i < N; i++) {
				PowerRect top = new PowerRect(
						new Point2D(p1.x + (p2.x - p1.x) * i, p1.y + (p2.y - p1.y) * i),
						new Point2D(p2.x + (p2.x - p1.x) * i, p2.y + (p2.y - p1.y) * i),
						new Point2D(p3.x + (p2.x - p1.x) * i, p3.y + (p2.y - p1.y) * i),
						new Point2D(p4.x + (p2.x - p1.x) * i, p4.y + (p2.y - p1.y) * i)
				);
				rects.add(top);
				neutralRects.add(top);
			}
			// 00
			PowerRect r00 = new PowerRect(new Point2D(0, 0),new Point2D(800, 0),new Point2D(800, 800),new Point2D(0, 800));
			rects.add(r00);
			topRects.add(r00);
			// X0
			PowerRect rX0 =  new PowerRect(new Point2D(2800, 0),new Point2D(3999, 0),new Point2D(4000, 1),new Point2D(4000, 1200));
			rects.add(rX0);
			middleRects.add(rX0);
			//bottomRects.add(rX0);
			//topRects.add(rX0);
			// 0Y
			PowerRect r0Y = new PowerRect(new Point2D(0, 2800),new Point2D(1200, 4000),new Point2D(1, 4000),new Point2D(0, 3999));
			rects.add(r0Y);
			middleRects.add(r0Y);
			//bottomRects.add(r0Y);
			//topRects.add(r0Y);
			// XY
			PowerRect rXY = new PowerRect(new Point2D(3200, 3200),new Point2D(4000, 3200),new Point2D(4000, 4000),new Point2D(3200, 4000));
			rects.add(rXY);
			bottomRects.add(rXY);
			// center
			PowerRect center = new PowerRect(new Point2D(1600, 2000),new Point2D(2000, 1600),new Point2D(2400, 2000),new Point2D(2000, 2400)); 
			rects.add(center);
			middleRects.add(center);
			
			
			
			Comparator<PowerRect> comparator = new Comparator<PowerRect>() {
				public int compare(PowerRect arg0, PowerRect arg1) {
					return arg0.getCenter().getDistanceTo(HOME_POINT.point) - arg1.getCenter().getDistanceTo(HOME_POINT.point) < 0 ? -1 : 1;
				}
			};
			middleRects.sort(comparator);
			bottomRects.sort(comparator);
			topRects.sort(comparator);
		}
		
		PowerRect getNearestBottomEnemy() {
			for (PowerRect r: bottomRects)
				if (r.enemyPowerInRect > 0)
					return r;
			
			return bottomRects.get(bottomRects.size() - 1);
		}
		
		PowerRect getFurtherBottomFriend() {
			for (int i = bottomRects.size() - 1; i >= 0; i--) {
				PowerRect r = bottomRects.get(i);
					if (r.friendPowerInRect > 0)
						return r;
			}
			
			return bottomRects.get(0);
		}
		
		PowerRect getNearestTopEnemy() {
			for (PowerRect r: topRects)
				if (r.enemyPowerInRect > 0)
					return r;
			
			return topRects.get(topRects.size() - 1);
		}
		
		PowerRect getFurtherTopFriend() {
			for (int i = topRects.size() - 1; i >= 0; i--) {
				PowerRect r = topRects.get(i);
					if (r.friendPowerInRect > 0)
						return r;
			}
			
			return topRects.get(0);
		}
		
		PowerRect getNearestMiddleEnemy() {
			for (int i = 1; i < middleRects.size(); i++) {
				PowerRect r = middleRects.get(i);
				if (r.enemyPowerInRect > 0)
					return r;
			}
			
			return middleRects.get(middleRects.size() - 1);
		}
		
		PowerRect getFurtherMiddleFriend() {
			for (int i = middleRects.size() - 2; i >= 1; i--) {
				PowerRect r = middleRects.get(i);
					if (r.friendPowerInRect > 0)
						return r;
			}
			
			return middleRects.get(0);
		}
		
		Point2D getNextSafePointByLane(LaneType lane) {
			PowerRect bottom = POWER_MAP.getNextBottom();
			PowerRect top = POWER_MAP.getNextTop();
			PowerRect middle = POWER_MAP.getNextMiddle();
			
			
			int indBottom = POWER_MAP.bottomRects.indexOf(bottom);
			Point2D bottomPoint = indBottom <= 1 ? bottomRects.get(0).getCenter() : POWER_MAP.bottomRects.get(indBottom - 2).getCenter();
			
			int indTop = POWER_MAP.topRects.indexOf(top);
			Point2D topPoint = indTop <= 1 ? topRects.get(0).getCenter() : POWER_MAP.topRects.get(indTop - 2).getCenter();
			
			int indMiddle = POWER_MAP.middleRects.indexOf(middle);
			Point2D middlePoint = indMiddle <= 1 ? middleRects.get(0).getCenter() : POWER_MAP.middleRects.get(indMiddle - 2).getCenter();
			
			
			if (lane == LaneType.BOTTOM)
				return bottomPoint;
			if (lane == LaneType.TOP)
				return topPoint;
			return middlePoint; 
		}
		
		PowerRect getNextBottom() {
			PowerRect e = POWER_MAP.getNearestBottomEnemy();
			PowerRect f = POWER_MAP.getFurtherBottomFriend();
			PowerRect res;
			
    		if ( e.getCenter().getDistanceTo(HOME_POINT.point) < f.getCenter().getDistanceTo(HOME_POINT.point) ) {
    			res = e;
    		}
    		else {
    			int i = bottomRects.indexOf(f);
    			if (i != bottomRects.size() - 1)
    				res = bottomRects.get(i+1);
    			else 
    				res = bottomRects.get(i);
    		}		
    		
    		int i = bottomRects.indexOf(res);
			if (res.contains(new Point2D(SELF)))
				if (i != bottomRects.size() - 1)
					return bottomRects.get(i+1);
				else
					return middleRects.get(middleRects.size() - 1);
			else 
				return res;
		}
		
		PowerRect getNextTop() {
			PowerRect e = POWER_MAP.getNearestTopEnemy();
			PowerRect f = POWER_MAP.getFurtherTopFriend();
			PowerRect res;
			
    		if ( e.getCenter().getDistanceTo(HOME_POINT.point) < f.getCenter().getDistanceTo(HOME_POINT.point) ) {
    			res = e;
    		}
    		else {
    			int i = topRects.indexOf(f);
    			if (i != topRects.size() - 1)
    				res = topRects.get(i+1);
    			else 
    				res = topRects.get(i);
    		}		
    		
    		int i = topRects.indexOf(res);
			if (res.contains(new Point2D(SELF)))
				if (i != topRects.size() - 1)
					return topRects.get(i+1);
				else
					return middleRects.get(middleRects.size() - 1);
			else 
				return res;
		}
		
		PowerRect getNextMiddle() {
			PowerRect e = POWER_MAP.getNearestMiddleEnemy();
			PowerRect f = POWER_MAP.getFurtherMiddleFriend();
			PowerRect res;

    		if ( e.getCenter().getDistanceTo(HOME_POINT.point) < f.getCenter().getDistanceTo(HOME_POINT.point) ) {
    			res = e;
    		}
    		else {
    			int i = middleRects.indexOf(f);
    			if (i != middleRects.size() - 1)
    				res = middleRects.get(i+1);
    			else 
    				res = middleRects.get(i);
    		}		
    		
    		int i = middleRects.indexOf(res);
			if (res.contains(new Point2D(SELF)))
				if (i != middleRects.size() - 1)
					return middleRects.get(i+1);
				else
					return middleRects.get(middleRects.size() - 1);
			else 
				return res;
		}
		
		PowerRect getNextByLane(LaneType lane) {
			if (lane == LaneType.BOTTOM)
				return getNextBottom();
			if (lane == LaneType.MIDDLE) 
				return getNextMiddle();
			return getNextTop();
		}
		
	    void calculateMapPower() {
	    	for (PowerRect r: rects) {
	    		r.enemyPowerInRect = r.friendPowerInRect = r.enemyPowerOnRect 
	    				= r.friendPowerOnRect = r.enemyPower = r.friendPower = 0;
	    		
	    		for (LivingUnit u: enemyUnits) {
	    			double dpm = getUnitDPM(u);
	    			if (r.contains(u) || r.getCenter().getDistanceTo(u) < getUnitRange(u)) 
	    				r.enemyPower += dpm;
	    			if (r.contains(u)) 
	    				r.enemyPowerInRect += dpm;
	    			if (r.getCenter().getDistanceTo(u) < getUnitRange(u))
	    				r.enemyPowerOnRect += dpm;
	    		}
	    		
	    		for (LivingUnit u: friendUnits) {
	    			double dpm = getUnitDPM(u);
	    			if (r.contains(u) || r.getCenter().getDistanceTo(u) < getUnitRange(u)) 
	    				r.friendPower += dpm;
	    			if (r.contains(u)) 
	    				r.friendPowerInRect += dpm;
	    			if (r.getCenter().getDistanceTo(u) < getUnitRange(u))
	    				r.friendPowerOnRect += dpm;
	    		}
	    		 
	    	}
	    	
	    	
	    	for (int i = 1; i < middleRects.size() - 1; i++) {
	    		middleRects.get(i + 1).enemyPowerInRect += middleRects.get(i).enemyPowerInRect;
	    		middleRects.get(i + 1).enemyPower += middleRects.get(i).enemyPower;
	    	}
	    	for (int i = 0; i < topRects.size() - 1; i++) {
	    		topRects.get(i + 1).enemyPowerInRect += topRects.get(i).enemyPowerInRect;
	    		topRects.get(i + 1).enemyPower += topRects.get(i).enemyPower;
	    	}
	    	for (int i = 0; i < bottomRects.size() - 1; i++) {
	    		bottomRects.get(i + 1).enemyPowerInRect += bottomRects.get(i).enemyPowerInRect;
	    		bottomRects.get(i + 1).enemyPower += bottomRects.get(i).enemyPower;
	    	}
	    }
	    
	    boolean isSegmentSafe(Segment s, Point2D exclude) {
	    	List<PowerRect> powerRects = new ArrayList<PowerRect>();
	    	powerRects.addAll(getRectsOnWay(s));
	    	
	    	for (PowerRect r: powerRects)
	    		if (!r.contains(new Point2D(SELF)) 
	    				&& !r.contains(exclude)
	    				&& r.enemyPowerInRect > 0 && r.friendPowerInRect == 0) {
	    			return false;
	    		}
	    	
	    	return true;
	    }

	    boolean isWaySafe(List<Waypoint> way) {
	    	if (way.size() == 0)
	    		return true;
	    	
	    	List<Segment> segments = new ArrayList<Segment>();
	    	List<PowerRect> powerRects = new ArrayList<PowerRect>();
	    	
	    	segments.add(new Segment(new Point2D(SELF), way.get(0).point));
	    	
	    	for (int i = 1; i < way.size() - 1; i++)
	    		segments.add(new Segment(way.get(i-1).point, way.get(i).point));
	    	
	    	for (Segment s: segments)
	    		powerRects.addAll(getRectsOnWay(s));
	    	
	    	for (PowerRect r: powerRects)
	    		if (!r.contains(new Point2D(SELF)) && r.enemyPowerInRect > 0) {
	    			return false;
	    		}

	    	return true;
	    }
	    
	    PowerRect getMyRect() {
	    	return getPointRect(new Point2D(SELF));
	    }
	    
	    PowerRect getPointRect(Point2D p) {
	    	for (PowerRect r: topRects)
	    		if (r.contains(p))
	    			return r;
	    	
	    	for (PowerRect r: bottomRects)
	    		if (r.contains(p))
	    			return r;
	    	
	    	for (PowerRect r: middleRects)
	    		if (r.contains(p))
	    			return r;	
	    	
	    	for (PowerRect r: neutralRects)
	    		if (r.contains(p))
	    			return r;
	    	
	    	return null;
	    }
	    
	    LaneType getRectLane(PowerRect r) {
	    	for (PowerRect a: topRects)
	    		if (a == r)
	    			return LaneType.TOP;
	    	
	    	for (PowerRect a: bottomRects)
	    		if (a == r)
	    			return LaneType.BOTTOM;
	    	
	    	for (PowerRect a: middleRects)
	    		if (a == r)
	    			return LaneType.MIDDLE;
	    	
	    	return null;
	    }
	    
	    
	    
	    List<PowerRect> getRectsByLane(LaneType l) {
	    	if (l == LaneType.TOP)
	    		return topRects;
	    	if (l == LaneType.BOTTOM)
	    		return bottomRects;
	    	if (l == LaneType.MIDDLE)
	    		return middleRects;
	    	return null;
	    }
	    
	    LaneType getPointLane(Point2D r) {
	    	for (PowerRect a: topRects)
	    		if (a.contains(r))
	    			return LaneType.TOP;
	    	
	    	for (PowerRect a: bottomRects)
	    		if (a.contains(r))
	    			return LaneType.BOTTOM;
	    	
	    	for (PowerRect a: middleRects)
	    		if (a.contains(r))
	    			return LaneType.MIDDLE;
	    	
	    	
	    	return null;
	    }
	    
	    List<PowerRect> getRectsOnWay(Segment way) {
	    	List<PowerRect> powerRects = new ArrayList<PowerRect>();

    		for (PowerRect r: this.rects) {
    			if ( r.isIntersect(way) || r.contains(way.line.start) || r.contains(way.line.end) ) {
    				powerRects.add(r);
    			}
    		}
	    	
	    	return powerRects;
	    }
	}
	
	
	
	class PowerRect extends Rectangle{
    	double friendPowerInRect = 0, enemyPowerInRect = 0;
    	double friendPowerOnRect = 0, enemyPowerOnRect = 0;
    	double enemyPower = 0, friendPower = 0;

    	PowerRect(Point2D p1, Point2D p2, Point2D p3, Point2D p4) {
			super(p1,p2,p3,p4);
		}
    	
    	double getPowerInRect() {
    		return friendPowerInRect - enemyPowerInRect;
    	}
    	
    	double getPowerOnRect() {
    		return friendPowerOnRect - enemyPowerOnRect;
    	}
    	
    	double getEnemyPower() {
    		return enemyPower;
    	}
    	
	}
	
	
}