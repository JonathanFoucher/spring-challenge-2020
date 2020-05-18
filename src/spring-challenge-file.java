// retirer les collect size => count()
// retirer collect foreach

// TODO => gestion des pacs morts
// TODO => mécanisme de défense (switch)
// TODO => fuite opposé quand ennemi si tuyau (+ save conscience fuite)
// TODO => gestion boost optimal

// TODO PAS BOOST -1 AVANT INTER (cas croisement avec )

// todo if speed 2 => viser 2e pastille

import java.util.*;
import java.io.*;
import java.math.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Grab the pellets as fast as you can!
 **/
class Player {
    public static List<Cell> cells = new ArrayList<Cell>();
    public static int width;
    public static int height;

    public static List<Pac> pacs = new ArrayList<Pac>();
    public static List<Pac> inSightEnnemies = new ArrayList<Pac>();

    public static List<Intersection> intersections = new ArrayList<Intersection>();
    public static List<Pipe> pipes = new ArrayList<Pipe>();

    public static int round = 1;

    public static void main(String args[]) {
        cells = new ArrayList<Cell>();

        Scanner in = new Scanner(System.in);
        width = in.nextInt(); // size of the grid
        height = in.nextInt(); // top left corner is (x=0, y=0)
        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; i++) {
            String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for (int j = 0; j < width; j++) {
                if (row.charAt(j) == ' ') {
                    cells.add(new Cell(j, i, 1));
                }
            }
        }

        for (int i = 0; i < cells.size(); i++) {
            Cell actualCell = cells.get(i);
            int x = actualCell.getX();
            int y = actualCell.getY();

            if (cells.stream()
                    .filter(c -> (c.isPosition((x + 1) % Player.width, y)
                            || c.isPosition((x - 1 + Player.width) % Player.width, y)
                            || c.isPosition(x, (y + 1) % Player.height)
                            || c.isPosition(x, (y - 1 + Player.height) % Player.height)))
                    .collect(Collectors.toList()).size() > 2) {
                Intersection intersection = new Intersection(actualCell);
                actualCell.setIsIntersection(true);
                actualCell.setIntersection(intersection);
                intersections.add(intersection);
            }
        }

        for (int i = 0; i < intersections.size(); i++) {
            Intersection intersection = intersections.get(i);
            int xI = intersection.getCell().getX();
            int yI = intersection.getCell().getY();

            List<Cell> cellsNextTo = cells.stream()
                    .filter(c -> (c.isPosition((xI + 1) % Player.width, yI)
                            || c.isPosition((xI - 1 + Player.width) % Player.width, yI)
                            || c.isPosition(xI, (yI + 1) % Player.height)
                            || c.isPosition(xI, (yI - 1 + Player.height) % Player.height)))
                    .collect(Collectors.toList());

            for (int j = 0; j < cellsNextTo.size(); j++) {
                Cell firstCell = cellsNextTo.get(j);
                Pipe pipe = pipes.stream().filter(p -> p.containsCell(firstCell)).findAny().orElse(null);
                Intersection nextIntersection;

                if (pipe == null) {
                    List<Cell> pipeCells = new ArrayList<Cell>();
                    Cell nextCell = firstCell;

                    while (nextCell != null && !nextCell.isIntersection()) {
                        pipeCells.add(nextCell);

                        int x = nextCell.getX();
                        int y = nextCell.getY();

                        nextCell = cells.stream()
                                .filter(c -> (c.isPosition((x + 1) % Player.width, y)
                                        || c.isPosition((x - 1 + Player.width) % Player.width, y)
                                        || c.isPosition(x, (y + 1) % Player.height)
                                        || c.isPosition(x, (y - 1 + Player.height) % Player.height))
                                        && !pipeCells.contains(c) && !c.isPosition(xI, yI))
                                .findAny().orElse(null);
                    }

                    Cell endCell = nextCell;

                    nextIntersection = endCell != null ? intersections.stream()
                            .filter(inter -> inter.getCell().isPosition(endCell.getX(), endCell.getY())).findAny().get()
                            : null;

                    pipe = new Pipe(pipeCells);
                    pipe.addIntersection(intersection);
                    if (nextIntersection != null) {
                        pipe.addIntersection(nextIntersection);
                    }

                    pipes.add(pipe);

                    Pipe finalPipe = pipe;
                    pipe.getCells().stream().forEach(c -> c.setPipe(finalPipe));
                } else {
                    nextIntersection = pipe.getIntersections().stream().filter(inter -> inter != intersection).findAny()
                            .orElse(null);
                }

                intersection.addPathsMap(pipe, nextIntersection);
            }
        }

        int myScore = in.nextInt();
        int opponentScore = in.nextInt();

        List<Integer> visibleEnnemiesIds = new ArrayList<Integer>();

        int visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
        for (int i = 0; i < visiblePacCount; i++) {
            int pacId = in.nextInt(); // pac number (unique within a team)
            boolean mine = in.nextInt() != 0; // true if this pac is yours
            int x = in.nextInt(); // position in the grid
            int y = in.nextInt(); // position in the grid
            String typeId = in.next(); // unused in wood leagues
            int speedTurnsLeft = in.nextInt(); // unused in wood leagues
            int abilityCooldown = in.nextInt(); // unused in wood leagues

            if (mine) {
                Cell spawnCell = Player.cells.stream().filter(c -> c.isPosition(x, y)).findAny().get();
                pacs.add(new Pac(pacId, x, y, mine, typeId));
                Cell symCell = getSymetricCell(spawnCell);
                pacs.add(new Pac(pacId, symCell.getX(), symCell.getY(), !mine, typeId));
            } else {
                visibleEnnemiesIds.add(pacId);
            }
        }

        visibleEnnemiesIds.forEach(pacId -> {
            Pac foundPac = pacs.stream().filter(p -> p.getId() == pacId && p.isMine() == false).findAny().get();
            inSightEnnemies.add(foundPac);
        });

        int visiblePelletCount = in.nextInt(); // all pellets in sight
        for (int i = 0; i < visiblePelletCount; i++) {
            int x = in.nextInt();
            int y = in.nextInt();
            int value = in.nextInt(); // amount of points this pellet is worth

            Cell actualCell = cells.stream().filter(c -> c.isPosition(x, y)).findAny().get();
            actualCell.setPoints(value);
            if (value > 1) {
                actualCell.searchClosestPac().addTargetedBigPellets(actualCell);
            }
        }

        inSightEnnemies.forEach(p -> p.getCurrentCell().searchClosestAlliedPac().actionOnEnnemy(p));

        inSightEnnemies.removeAll(inSightEnnemies);

        pacs.stream().filter(c -> c.isMine() && "".equals(c.getAction())).collect(Collectors.toList())
                .forEach(p -> p.nextAction());

        System.out.println(String.join(" | ",
                pacs.stream().filter(p -> !"".equals(p.getAction())).map(Pac::getAction).collect(Collectors.toList())));

        // game loop
        while (true) {
            round++;

            intersections.stream().filter(i -> i.isTaken()).forEach(i -> i.setIsTaken(false));

            resetEnemiesPositions();
            pacs.stream().filter(p -> p.isMine()).forEach(Pac::resetAction);
            cells.stream().forEach(c -> c.setPacOnCell(null));

            myScore = in.nextInt();
            opponentScore = in.nextInt();

            List<Pac> myAlivePacs = new ArrayList<Pac>();

            visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt(); // pac number (unique within a team)
                boolean mine = in.nextInt() != 0; // true if this pac is yours
                int x = in.nextInt(); // position in the grid
                int y = in.nextInt(); // position in the grid
                String typeId = in.next(); // unused in wood leagues
                int speedTurnsLeft = in.nextInt(); // unused in wood leagues
                int abilityCooldown = in.nextInt(); // unused in wood leagues

                Pac foundPac = pacs.stream().filter(p -> p.getId() == pacId && p.isMine() == mine).findAny().get();

                if (mine) {
                    myAlivePacs.add(foundPac);
                } else {
                    inSightEnnemies.add(foundPac);
                }

                foundPac.updateRound(x, y, typeId, speedTurnsLeft, abilityCooldown);
            }

            checkDeaths(myAlivePacs);

            visiblePelletCount = in.nextInt(); // all pellets in sight

            resetBigPellets();

            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth

                Cell actualCell = cells.stream().filter(c -> c.isPosition(x, y)).findAny().get();
                actualCell.setPoints(value);
                if (value > 1 && !pacs.stream().anyMatch(p -> p.getTargetedBigPellets().contains(actualCell))) {
                    actualCell.searchClosestPac().addTargetedBigPellets(actualCell);
                }
            }

            pacs.stream().forEach(Pac::refreshBigTargets);

            inSightEnnemies.stream().forEach(p -> p.getCurrentCell().searchClosestAlliedPac().actionOnEnnemy(p));

            inSightEnnemies.removeAll(inSightEnnemies);

            pacs.stream().filter(p -> p.isMine() && "".equals(p.getAction())).collect(Collectors.toList())
                    .forEach(p -> p.nextAction());

            System.out.println(String.join(" | ", pacs.stream().filter(p -> !"".equals(p.getAction()))
                    .map(Pac::getAction).collect(Collectors.toList())));
        }
    }

    public static Cell getSymetricCell(Cell cell) {
        return cells.stream().filter(c -> c.getX() + 1 == (width - cell.getX()) && c.getY() == cell.getY()).findAny()
                .get();
    }

    public static void resetEnemiesPositions() {
        pacs.stream().filter(p -> !p.isMine() && p.getCurrentCell() != null).forEach(p -> p.disappear());
    }

    public static void checkDeaths(List<Pac> myAlivePacs) {
        pacs.stream().filter(p -> p.isMine() && !myAlivePacs.contains(p)).forEach(Pac::kill);
        pacs.removeIf(p -> p.isMine() && !myAlivePacs.contains(p));
    }

    public static void resetBigPellets() {
        cells.stream().filter(c -> c.getPoints() > 1).forEach(c -> c.setPoints(0));
    }
}

class Pac {
    private int id;
    private Cell currentCell;
    private boolean mine;
    private List<Cell> targetedBigPellets;
    private String action;
    private String typeId;
    private int speedTurnsLeft;
    private int abilityCooldown;
    private int speed;
    private List<Cell> previousCells;
    private boolean isInDanger;

    public Pac(int id, int x, int y, boolean mine, String typeId) {
        this.id = id;
        this.mine = mine;
        this.previousCells = new ArrayList<Cell>();
        this.targetedBigPellets = new ArrayList<Cell>();
        this.action = "";
        this.typeId = typeId;
        this.speed = 1;
        setNewCell(x, y);
        this.currentCell.setPoints(0);
        this.isInDanger = false;
    }

    public int getId() {
        return this.id;
    }

    public Cell getCurrentCell() {
        return this.currentCell;
    }

    public boolean isMine() {
        return this.mine;
    }

    public void kill() {
        Player.pipes.stream().filter(p -> this.equals(p.isTakenBy())).findAny().ifPresent(p -> p.setIsTakenBy(null));
        if (this.currentCell.getIntersection() != null) {
            this.currentCell.getIntersection().setIsTaken(false);
        }

        this.currentCell.setPacOnCell(null);
    }

    public List<Cell> getTargetedBigPellets() {
        return this.targetedBigPellets;
    }

    public void addTargetedBigPellets(Cell target) {
        this.targetedBigPellets.add(target);
    }

    public boolean isAtIntersection() {
        return currentCell.isIntersection();
    }

    public void resetAction() {
        this.action = "";
    }

    public String getAction() {
        return this.action;
    }

    public String getTypeId() {
        return this.typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public int getSpeedTurnsLeft() {
        return speedTurnsLeft;
    }

    public int getAbilityCooldown() {
        return abilityCooldown;
    }

    public int getSpeed() {
        return speed;
    }

    public void updateRound(int x, int y, String typeId, int speedTurnsLeft, int abilityCooldown) {
        setNewCell(x, y);
        this.typeId = typeId;
        this.speedTurnsLeft = speedTurnsLeft;
        if (speedTurnsLeft > 0) {
            this.speed = 2;
        } else {
            this.speed = 1;
        }

        this.abilityCooldown = abilityCooldown;

        if (this.mine) {
            this.updateVision();
        }

        this.isInDanger = false;
    }

    public void updateVision() {
        Cell startCell = this.currentCell;
        int x = startCell.getX();
        int y = startCell.getY();

        // CLEAN TOP
        Cell topCell = Player.cells.stream().filter(c -> c.isPosition(x, (y - 1 + Player.height) % Player.height))
                .findAny().orElse(null);
        while (topCell != null) {
            topCell.setPoints(0);
            Cell tempCell = topCell;
            topCell = Player.cells.stream()
                    .filter(c -> c.isPosition(tempCell.getX(), (tempCell.getY() - 1 + Player.height) % Player.height))
                    .findAny().orElse(null);
        }

        // CLEAN BOT
        Cell botCell = Player.cells.stream().filter(c -> c.isPosition(x, (y + 1) % Player.height)).findAny()
                .orElse(null);
        while (botCell != null) {
            botCell.setPoints(0);
            Cell tempCell = botCell;
            botCell = Player.cells.stream()
                    .filter(c -> c.isPosition(tempCell.getX(), (tempCell.getY() + 1) % Player.height)).findAny()
                    .orElse(null);
        }

        // CLEAN LEFT
        Cell leftCell = Player.cells.stream().filter(c -> c.isPosition((x - 1 + Player.width) % Player.width, y))
                .findAny().orElse(null);
        while (leftCell != null && leftCell != startCell) {
            leftCell.setPoints(0);
            Cell tempCell = leftCell;
            leftCell = Player.cells.stream()
                    .filter(c -> c.isPosition((tempCell.getX() - 1 + Player.width) % Player.width, tempCell.getY()))
                    .findAny().orElse(null);
        }

        // CLEAN RIGHT
        Cell rigthCell = Player.cells.stream().filter(c -> c.isPosition((x + 1) % Player.width, y)).findAny()
                .orElse(null);
        while (rigthCell != null && rigthCell != startCell) {
            rigthCell.setPoints(0);
            Cell tempCell = rigthCell;
            rigthCell = Player.cells.stream()
                    .filter(c -> c.isPosition((tempCell.getX() + 1) % Player.width, tempCell.getY())).findAny()
                    .orElse(null);
        }

    }

    public void activateSpeed() {
        this.action = "SPEED " + this.id;
    }

    public void switchTo(String typeId) {
        this.action = "SWITCH " + this.id + " " + typeId;
    }

    public void moveTo(Cell cell) {
        if (cell != null) {
            this.action = "MOVE " + this.id + " " + cell.getX() + " " + cell.getY();

            if (cell.isIntersection()) {
                Player.pipes.stream().filter(p -> this.equals(p.isTakenBy())).forEach(p -> p.setIsTakenBy(null));
            } else {
                cell.getPipe().setIsTakenBy(this);
            }

            Pipe pipe = currentCell.getPipe();
            if (pipe != null) {
                int pos = pipe.getCells().indexOf(currentCell);
                if (pos == 0 || pos == pipe.getLength() - 1) {
                    Intersection intersection = pipe.getIntersections().get(0);
                    Cell c = intersection.getCell();

                    if (pipe.getIntersections().size() > 1
                            && !(c.isPosition((currentCell.getX() + 1) % Player.width, currentCell.getY())
                                    || c.isPosition((currentCell.getX() - 1 + Player.width) % Player.width,
                                            currentCell.getY())
                                    || c.isPosition(currentCell.getX(), (currentCell.getY() + 1) % Player.height)
                                    || c.isPosition(currentCell.getX(),
                                            (currentCell.getY() - 1 + Player.height) % Player.height))) {
                        intersection = pipe.getIntersections().get(1);
                    }

                    if (c.isPosition((currentCell.getX() + 1) % Player.width, currentCell.getY())
                            || c.isPosition((currentCell.getX() - 1 + Player.width) % Player.width, currentCell.getY())
                            || c.isPosition(currentCell.getX(), (currentCell.getY() + 1) % Player.height)
                            || c.isPosition(currentCell.getX(),
                                    (currentCell.getY() - 1 + Player.height) % Player.height)) {
                        if (intersection.isTaken()) {
                            this.action = "";
                        } else {
                            intersection.setIsTaken(true);
                        }
                    }

                }
            }
        }
    }

    public void setNewCell(int x, int y) {
        Cell cell = Player.cells.stream().filter(c -> c.isPosition(x, y)).findAny().get();

        this.previousCells.add(cell);
        if (this.previousCells.size() > 10) {
            this.previousCells.remove(0);
        }

        cell.setPoints(0);
        this.currentCell = cell;
        cell.setPacOnCell(this);

        this.targetedBigPellets.removeIf(c -> cell.equals(c));
    }

    public void disappear() {
        this.currentCell.setPacOnCell(null);
        this.currentCell = null;
        this.targetedBigPellets.removeAll(this.targetedBigPellets);
    }

    public Cell getClosestPelletCell() {
        if (this.targetedBigPellets.size() > 0) {
            return this.targetedBigPellets.get(0);
        }

        Cell closestTarget = null;
        final int x = this.currentCell.getX();
        final int y = this.currentCell.getY();

        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = Player.cells.stream()
                .filter(c -> (c.isPosition((x + 1) % Player.width, y)
                        || c.isPosition((x - 1 + Player.width) % Player.width, y)
                        || c.isPosition(x, (y + 1) % Player.height)
                        || c.isPosition(x, (y - 1 + Player.height) % Player.height))
                        && ((c.isIntersection() && !c.getIntersection().isTaken())
                                || (!c.isIntersection() && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null))))
                .collect(Collectors.toList());

        do {
            List<Cell> futurCells = new ArrayList<Cell>();
            List<Cell> candidatsCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPoints() > 0 && actualCell.searchClosestAlliedPac().equals(this)) {
                    candidatsCells.add(actualCell);
                } else if (actualCell.getPoints() > 0 && closestTarget == null) {
                    closestTarget = actualCell;
                }

                futurCells.addAll(Player.cells.stream()
                        .filter(c -> (c.isPosition((actualCell.getX() + 1) % Player.width, actualCell.getY())
                                || c.isPosition((actualCell.getX() - 1 + Player.width) % Player.width,
                                        actualCell.getY())
                                || c.isPosition(actualCell.getX(), (actualCell.getY() + 1) % Player.height)
                                || c.isPosition(actualCell.getX(),
                                        (actualCell.getY() - 1 + Player.height) % Player.height))
                                && !alreadySeenCells.contains(c) && !futurCells.contains(c)
                                && ((c.isIntersection() && !c.getIntersection().isTaken())
                                        || (!c.isIntersection() && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null))))
                        .collect(Collectors.toList()));
            }

            if (candidatsCells.size() == 1) {
                return candidatsCells.get(0);
            } else if (candidatsCells.size() > 0) {
                Cell cellToTarget = candidatsCells.stream().filter(c -> c.isIntersection() || !c.getPipe().isDeadEnd())
                        .findAny().orElse(null);
                if (cellToTarget != null) {
                    return cellToTarget;
                } else {
                    return candidatsCells.get(0);
                }
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
        } while (actualCells.size() > 0);

        return closestTarget;
    }

    public void nextAction() {
        if (this.abilityCooldown == 0 && !this.isInDanger) {
            activateSpeed();
        } else if (!currentCell.isIntersection() && currentCell.getPipe().isDeadEnd()
                && currentCell.getPipe().getPointsCells() == 0) {
            moveTo(currentCell.getPipe().getIntersections().get(0).getCell());
        } else {
            moveTo(this.getClosestPelletCell());
        }
    }

    public void refreshBigTargets() {
        this.targetedBigPellets.removeIf(tbp -> tbp.getPoints() < 1);
    }

    public void actionOnEnnemy(Pac ennemyPac) {
        if (this.targetedBigPellets.size() == 0) {
            int distance = this.currentCell.getDistanceTo(ennemyPac.getCurrentCell());
            System.err.println(distance);
            boolean doPacWin = doPacWin(ennemyPac);
            String ennemyCounter = getCounter(ennemyPac.getTypeId());

            if (!doPacWin) {
                this.isInDanger = true;

                if (distance <= 5) {
                    flee(ennemyPac, distance);
                }
            }

            if ((ennemyPac.getSpeed() == distance) && !doPacWin && this.abilityCooldown == 0) {
                switchTo(ennemyCounter);
                return;
            }

            if (doPacWin && ennemyPac.getAbilityCooldown() > 0 && this.targetedBigPellets.isEmpty()) {
                chase(ennemyPac);
            } else if (!doPacWin && ennemyPac.getAbilityCooldown() > 0 && this.abilityCooldown == 0) {
                switchTo(ennemyCounter);
            }
        }
    }

    public boolean doPacWin(Pac ennemyPac) {
        return (ennemyPac.getTypeId().equals("SCISSORS") && typeId.equals("ROCK"))
                || (ennemyPac.getTypeId().equals("ROCK") && typeId.equals("PAPER"))
                || (ennemyPac.getTypeId().equals("PAPER") && typeId.equals("SCISSORS"));
    }

    public String getCounter(String type) {
        switch (type) {
            case "SCISSORS":
                return "ROCK";
            case "ROCK":
                return "PAPER";
            case "PAPER":
                return "SCISSORS";
            default:
                return "";
        }
    }

    public void chase(Pac ennemyPac) {
        moveTo(ennemyPac.getCurrentCell());
    }

    public void flee(Pac ennemyPac, int distance) {
        Cell fleeCell = this.currentCell.searchFleeCell(this, ennemyPac.getCurrentCell(), this.speed, distance);
        if (fleeCell != null)
            moveTo(fleeCell);
    }
}

class Cell {
    private int x;
    private int y;
    private int points;
    private boolean isIntersection;
    private Pac pacOnCell;
    private Pipe pipe;
    private Intersection intersection;

    public Cell(int x, int y, int points) {
        this.x = x;
        this.y = y;
        this.points = points;
        this.isIntersection = false;
        this.pacOnCell = null;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getPoints() {
        return this.points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isPosition(int x, int y) {
        if (x == this.x && y == this.y) {
            return true;
        }

        return false;
    }

    public boolean isIntersection() {
        return this.isIntersection;
    }

    public void setIsIntersection(boolean isIntersection) {
        this.isIntersection = isIntersection;
    }

    public Pac getPacOnCell() {
        return this.pacOnCell;
    }

    public void setPacOnCell(Pac pacOnCell) {
        this.pacOnCell = pacOnCell;
    }

    public Intersection getIntersection() {
        return intersection;
    }

    public void setIntersection(Intersection intersection) {
        this.intersection = intersection;
    }

    public Pipe getPipe() {
        return pipe;
    }

    public void setPipe(Pipe pipe) {
        this.pipe = pipe;
    }

    public Pac searchClosestPac() {
        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = Player.cells.stream().filter(c -> (c.isPosition((x + 1) % Player.width, y)
                || c.isPosition((x - 1 + Player.width) % Player.width, y) || c.isPosition(x, (y + 1) % Player.height)
                || c.isPosition(x, (y - 1 + Player.height) % Player.height))).collect(Collectors.toList());

        do {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPacOnCell() != null) {
                    return actualCell.getPacOnCell();
                }

                futurCells.addAll(Player.cells.stream()
                        .filter(c -> (c.isPosition((actualCell.getX() + 1) % Player.width, actualCell.getY())
                                || c.isPosition((actualCell.getX() - 1 + Player.width) % Player.width,
                                        actualCell.getY())
                                || c.isPosition(actualCell.getX(), (actualCell.getY() + 1) % Player.height)
                                || c.isPosition(actualCell.getX(),
                                        (actualCell.getY() - 1 + Player.height) % Player.height))
                                && !alreadySeenCells.contains(c) && !futurCells.contains(c))
                        .collect(Collectors.toList()));
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
        } while (actualCells.size() > 0);

        return null;
    }

    public Pac searchClosestAlliedPac() {
        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = Player.cells.stream().filter(c -> (c.isPosition((x + 1) % Player.width, y)
                || c.isPosition((x - 1 + Player.width) % Player.width, y) || c.isPosition(x, (y + 1) % Player.height)
                || c.isPosition(x, (y - 1 + Player.height) % Player.height))).collect(Collectors.toList());

        do {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPacOnCell() != null && actualCell.getPacOnCell().isMine()) {
                    return actualCell.getPacOnCell();
                }

                futurCells.addAll(Player.cells.stream()
                        .filter(c -> (c.isPosition((actualCell.getX() + 1) % Player.width, actualCell.getY())
                                || c.isPosition((actualCell.getX() - 1 + Player.width) % Player.width,
                                        actualCell.getY())
                                || c.isPosition(actualCell.getX(), (actualCell.getY() + 1) % Player.height)
                                || c.isPosition(actualCell.getX(),
                                        (actualCell.getY() - 1 + Player.height) % Player.height))
                                && !alreadySeenCells.contains(c) && !futurCells.contains(c))
                        .collect(Collectors.toList()));
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
        } while (actualCells.size() > 0);

        return null;
    }

    public Cell searchFleeCell(Pac allyPac, Cell ennemyCell, int speed, int distance) {
        List<Cell> possibleCells = Player.cells.stream()
                .filter(c -> (c.isPosition((x + 1) % Player.width, y)
                        || c.isPosition((x - 1 + Player.width) % Player.width, y)
                        || c.isPosition(x, (y + 1) % Player.height)
                        || c.isPosition(x, (y - 1 + Player.height) % Player.height))
                        && ((c.isIntersection() && !c.getIntersection().isTaken())
                                || (!c.isIntersection() && (c.getPipe().isTakenBy() == allyPac || c.getPipe().isTakenBy() == null))))
                .collect(Collectors.toList());

        if (speed == 2) {
            List<Cell> newCells = new ArrayList<Cell>();
            List<Cell> tempCells = possibleCells;

            for (int i = 0; i < possibleCells.size(); i++) {
                Cell actualCell = possibleCells.get(i);

                newCells.addAll(Player.cells.stream()
                        .filter(c -> (c.isPosition((actualCell.getX() + 1) % Player.width, actualCell.getY())
                                || c.isPosition((actualCell.getX() - 1 + Player.width) % Player.width,
                                        actualCell.getY())
                                || c.isPosition(actualCell.getX(), (actualCell.getY() + 1) % Player.height)
                                || c.isPosition(actualCell.getX(),
                                        (actualCell.getY() - 1 + Player.height) % Player.height))
                                && !tempCells.contains(c) && !newCells.contains(c)
                                && ((c.isIntersection() && !c.getIntersection().isTaken())
                                        || (!c.isIntersection() && (c.getPipe().isTakenBy() == allyPac || c.getPipe().isTakenBy() == null))))
                        .collect(Collectors.toList()));
            }

            possibleCells = newCells;
        }

        possibleCells = possibleCells.stream().filter(c -> ennemyCell.getDistanceTo(c) > distance)
                .collect(Collectors.toList());

        Cell result = possibleCells.stream().filter(c -> c.isIntersection || !c.getPipe().isDeadEnd()).findAny()
                .orElse(null);

        if (result != null) {
            return result;
        }

        return possibleCells.size() > 0 ? possibleCells.get(0) : null;
    }

    public int getDistanceTo(Cell cell) {
        int distance = 1;
        boolean found = false;
        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = Player.cells.stream().filter(c -> (c.isPosition((x + 1) % Player.width, y)
                || c.isPosition((x - 1 + Player.width) % Player.width, y) || c.isPosition(x, (y + 1) % Player.height)
                || c.isPosition(x, (y - 1 + Player.height) % Player.height))).collect(Collectors.toList());

        while (true) {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.equals(cell)) {
                    found = true;
                    break;
                }

                futurCells.addAll(Player.cells.stream()
                        .filter(c -> (c.isPosition((actualCell.getX() + 1) % Player.width, actualCell.getY())
                                || c.isPosition((actualCell.getX() - 1 + Player.width) % Player.width,
                                        actualCell.getY())
                                || c.isPosition(actualCell.getX(), (actualCell.getY() + 1) % Player.height)
                                || c.isPosition(actualCell.getX(),
                                        (actualCell.getY() - 1 + Player.height) % Player.height))
                                && !alreadySeenCells.contains(c) && !futurCells.contains(c))
                        .collect(Collectors.toList()));
            }

            if (found)
                break;

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
            distance++;
        }

        return distance;
    }

    public void print() {
        System.err.println("x=" + this.x + " y=" + this.y + " pts=" + this.points + " pacOn="
                + (this.pacOnCell != null ? this.pacOnCell.isMine() + " " + this.pacOnCell.getId() : "null"));
    }
}

class Intersection {
    private Cell cell;
    private Map<Pipe, Intersection> pathsMap;
    private boolean isTaken;

    public Intersection(Cell cell) {
        this.cell = cell;
        this.pathsMap = new HashMap<Pipe, Intersection>();
        this.isTaken = false;
    }

    public Cell getCell() {
        return this.cell;
    }

    public Map<Pipe, Intersection> getPathsMap() {
        return this.pathsMap;
    }

    public void addPathsMap(Pipe pipe, Intersection intersection) {
        this.pathsMap.put(pipe, intersection);
    }

    public boolean isTaken() {
        return isTaken;
    }

    public void setIsTaken(boolean isTaken) {
        this.isTaken = isTaken;
    }
}

class Pipe {
    private List<Cell> cells;
    private List<Intersection> intersections;
    private Pac isTakenBy;

    public Pipe(List<Cell> cells) {
        this.cells = cells;
        this.intersections = new ArrayList<Intersection>();
        this.isTakenBy = null;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public int getLength() {
        return this.cells.size();
    }

    public boolean isDeadEnd() {
        return this.intersections.size() == 1;
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public void addIntersection(Intersection intersection) {
        this.intersections.add(intersection);
    }

    public boolean containsCell(Cell cell) {
        return this.cells.contains(cell);
    }

    public int getPointsCells() {
        return cells.stream().map(Cell::getPoints).reduce(0, Integer::sum);
    }

    public Pac isTakenBy() {
        return isTakenBy;
    }

    public void setIsTakenBy(Pac isTakenBy) {
        this.isTakenBy = isTakenBy;
    }
}
