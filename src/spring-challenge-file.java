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
    public static long totalCells;
    public static long remainingPallets;
    public static long consumedPallets = 0L;

    public static long limitCalculations;

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
            List<Cell> visionCells = new ArrayList<Cell>();

            for (int j = 0; j < width; j++) {
                if (row.charAt(j) == ' ') {
                    Cell actualCell = new Cell(j, i, 1);

                    cells.add(actualCell);

                    visionCells.add(actualCell);
                } else {
                    if (visionCells.size() > 0) {
                        List<Cell> finalVisionCells = visionCells;
                        visionCells.forEach(c -> c.addVisionCells(finalVisionCells));

                        visionCells = new ArrayList<Cell>();
                    }
                }

            }

            if (visionCells.size() > 0) {
                List<Cell> finalVisionCells = visionCells;
                visionCells.forEach(c -> c.addVisionCells(finalVisionCells));

                visionCells = new ArrayList<Cell>();
            }

            int finalI = i;
            if (cells.stream().anyMatch(c -> c.isPosition(0, finalI))) {
                List<Cell> vision1 = new ArrayList<>();

                vision1.addAll(cells.stream().filter(c -> c.isPosition(0, finalI)).findAny().get().getVisionCells());

                List<Cell> vision2 = new ArrayList<>();

                vision2.addAll(
                        cells.stream().filter(c -> c.isPosition(width - 1, finalI)).findAny().get().getVisionCells());

                vision1.forEach(c -> c.addVisionCells(vision2));
                vision2.forEach(c -> c.addVisionCells(vision1));
            }

        }

        for (int i = 0; i < width; i++) {
            List<Cell> visionCells = new ArrayList<Cell>();

            for (int j = 0; j < height; j++) {
                int finalI = i;
                int finalJ = j;
                Cell actualCell = cells.stream().filter(c -> c.isPosition(finalI, finalJ)).findAny().orElse(null);

                if (actualCell != null) {
                    visionCells.add(actualCell);
                } else if (actualCell == null || j == height - 1) {
                    if (visionCells.size() > 0) {
                        List<Cell> finalVisionCells = visionCells;
                        visionCells.forEach(c -> c.addVisionCells(finalVisionCells));

                        visionCells = new ArrayList<Cell>();
                    }
                }
            }
        }

        totalCells = cells.stream().filter(c -> c.getPoints() > 0).count();
        remainingPallets = totalCells;

        for (int i = 0; i < cells.size(); i++) {
            Cell actualCell = cells.get(i);
            int x = actualCell.getX();
            int y = actualCell.getY();

            if (cells.stream()
                    .filter(c -> (c.isPosition((x + 1) % Player.width, y)
                            || c.isPosition((x - 1 + Player.width) % Player.width, y)
                            || c.isPosition(x, (y + 1) % Player.height)
                            || c.isPosition(x, (y - 1 + Player.height) % Player.height)))
                    .count() > 2) {
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

            intersection.getCell().addNextCells(cellsNextTo);

            for (int j = 0; j < cellsNextTo.size(); j++) {
                Cell firstCell = cellsNextTo.get(j);
                Pipe pipe = pipes.stream().filter(p -> p.containsCell(firstCell)).findAny().orElse(null);
                Intersection nextIntersection;

                if (pipe == null) {
                    List<Cell> pipeCells = new ArrayList<Cell>();
                    Cell nextCell = firstCell;

                    while (nextCell != null && !nextCell.isIntersection()) {
                        Cell previousCell = nextCell;

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

                        if (nextCell != null) {
                            nextCell.addNextCell(previousCell);
                            previousCell.addNextCell(nextCell);
                        }
                    }

                    Cell endCell = nextCell;

                    nextIntersection = endCell != null ? intersections.stream()
                            .filter(inter -> inter.getCell().isPosition(endCell.getX(), endCell.getY())).findAny().get()
                            : null;

                    pipe = new Pipe(pipeCells);
                    pipe.addIntersection(intersection);
                    pipe.getCells().get(0).addNextCell(intersection.getCell());

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
            Pac foundPac = pacs.stream().filter(p -> p.getId() == pacId && !p.isMine()).findAny().get();
            inSightEnnemies.add(foundPac);
        });

        limitCalculations = 200 / Player.pacs.stream().filter(p -> p.isMine()).count();

        int visiblePelletCount = in.nextInt(); // all pellets in sight
        for (int i = 0; i < visiblePelletCount; i++) {
            int x = in.nextInt();
            int y = in.nextInt();
            int value = in.nextInt(); // amount of points this pellet is worth

            Cell actualCell = cells.stream().filter(c -> c.isPosition(x, y)).findAny().get();
            actualCell.setPoints(value);
        }

        analyzeBigPellets();
        analyzeBigPipes();

        inSightEnnemies.forEach(p -> p.getCurrentCell().searchClosestAlliedPac().actionOnEnnemy(p));

        inSightEnnemies.removeAll(inSightEnnemies);

        pacs.stream().filter(p -> p.isMine() && !p.getTypeId().equals("DEAD") && "".equals(p.getAction()))
                .collect(Collectors.toList()).forEach(p -> p.nextPriorityAction());

        pacs.stream().filter(p -> p.isMine() && !p.getTypeId().equals("DEAD") && "".equals(p.getAction()))
                .collect(Collectors.toList()).forEach(p -> p.nextAction());

        System.out.println(String.join(" | ",
                pacs.stream().filter(p -> !"".equals(p.getAction())).map(Pac::getAction).collect(Collectors.toList())));

        // game loop
        while (true) {
            round++;

            remainingPallets = cells.stream().filter(c -> c.getPoints() > 0).count();
            consumedPallets = totalCells - remainingPallets;

            intersections.stream().filter(i -> i.isTaken()).forEach(i -> i.setIsTaken(false));

            resetEnemiesPositions();
            pacs.stream().filter(p -> p.isMine()).forEach(Pac::resetAction);
            cells.stream().forEach(c -> c.setPacOnCell(null));

            myScore = in.nextInt();
            opponentScore = in.nextInt();

            visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt(); // pac number (unique within a team)
                boolean mine = in.nextInt() != 0; // true if this pac is yours
                int x = in.nextInt(); // position in the grid
                int y = in.nextInt(); // position in the grid
                String typeId = in.next(); // unused in wood leagues
                int speedTurnsLeft = in.nextInt(); // unused in wood leagues
                int abilityCooldown = in.nextInt(); // unused in wood leagues

                Pac foundPac = pacs.stream().filter(p -> p.getId() == pacId && p.isMine() == mine).findAny()
                        .orElse(null);

                if (foundPac != null) {

                    if (!mine && !typeId.equals("DEAD")) {
                        inSightEnnemies.add(foundPac);
                    }

                    foundPac.updateRound(x, y, typeId, speedTurnsLeft, abilityCooldown);

                    if (typeId.equals("DEAD")) {
                        foundPac.kill();
                        pacs.remove(foundPac);
                    }
                }
            }

            limitCalculations = 400 / Player.pacs.stream().filter(p -> p.isMine()).count();

            pacs.stream().forEach(p -> {
                p.getTargetedBigPellets().removeAll(p.getTargetedBigPellets());
            });

            visiblePelletCount = in.nextInt(); // all pellets in sight

            resetBigPellets();

            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth

                Cell actualCell = cells.stream().filter(c -> c.isPosition(x, y)).findAny().get();
                actualCell.setPoints(value);
                if (value > 1 && !pacs.stream().anyMatch(p -> p.getTargetedBigPellets().contains(actualCell)
                        || p.getObligatoryObjectives().contains(actualCell))) {
                    actualCell.searchClosestPac().addTargetedBigPellet(actualCell);
                }
            }

            pacs.stream().forEach(Pac::refreshBigTargets);

            pacs.stream().filter(p -> p.getCurrentCell() != null && !p.getTypeId().equals("DEAD"))
                    .forEach(p -> p.getCurrentCell().setPacOnCell(p));

            inSightEnnemies.stream().forEach(p -> p.getCurrentCell().searchClosestAlliedPac().actionOnEnnemy(p));

            inSightEnnemies.removeAll(inSightEnnemies);

            pacs.stream().filter(p -> p.isMine() && !p.getTypeId().equals("DEAD") && "".equals(p.getAction()))
                    .collect(Collectors.toList()).forEach(p -> p.nextPriorityAction());

            pacs.stream().filter(p -> p.isMine() && !p.getTypeId().equals("DEAD") && "".equals(p.getAction()))
                    .collect(Collectors.toList()).forEach(p -> p.nextAction());

            System.out.println(String.join(" | ", pacs.stream().filter(p -> !"".equals(p.getAction()))
                    .map(Pac::getAction).collect(Collectors.toList())));
        }
    }

    public static Cell getSymetricCell(Cell cell) {
        return cells.stream().filter(c -> c.getX() + 1 == (width - cell.getX()) && c.getY() == cell.getY()).findAny()
                .get();
    }

    public static void resetEnemiesPositions() {
        pacs.stream().filter(p -> !p.isMine() && !p.getTypeId().equals("DEAD") && p.getCurrentCell() != null)
                .forEach(p -> p.disappear());
    }

    public static void resetBigPellets() {
        cells.stream().filter(c -> c.getPoints() > 1).forEach(c -> c.setPoints(0));
    }

    public static void analyzeBigPellets() {
        List<Cell> bigPellets = cells.stream().filter(c -> c.getPoints() > 1).collect(Collectors.toList());

        List<DistancePacBigPellet> distPacsPellets = new ArrayList<DistancePacBigPellet>();

        for (int i = 0; i < bigPellets.size(); i++) {
            Cell actualCell = bigPellets.get(i);

            pacs.forEach(p -> distPacsPellets
                    .add(new DistancePacBigPellet(p.getCurrentCell().getDistanceTo(actualCell), p, actualCell)));
        }

        List<DistanceBetweenBigPellets> distBetweenBigPellets = new ArrayList<DistanceBetweenBigPellets>();

        for (int i = 0; i < bigPellets.size(); i++) {
            for (int j = 0; j < bigPellets.size(); j++) {
                if (i != j) {
                    Cell cell1 = bigPellets.get(i);
                    Cell cell2 = bigPellets.get(j);
                    distBetweenBigPellets.add(new DistanceBetweenBigPellets(cell1.getDistanceTo(cell2), cell1, cell2));
                }
            }
        }

        List<PossibilityObjective> possibilityObjectives = new ArrayList<PossibilityObjective>();

        pacs.forEach(p -> {
            for (int i = 0; i < bigPellets.size(); i++) {
                Cell cell1 = bigPellets.get(i);
                int calculateDistance = distPacsPellets.stream()
                        .filter(dpp -> dpp.getPac() == p && dpp.getCell() == cell1).findAny().get().getDistance();

                possibilityObjectives.add(new PossibilityObjective(cell1, calculateDistance, p));

                for (int j = 0; j < bigPellets.size(); j++) {
                    if (i != j) {
                        Cell cell2 = bigPellets.get(j);
                        int totalDistance = calculateDistance + distBetweenBigPellets.stream()
                                .filter(dbbp -> dbbp.containsCells(cell1, cell2)).findAny().get().getDistance();

                        possibilityObjectives.add(new PossibilityObjective(cell1, cell2, totalDistance, p));
                    }
                }
            }
        });

        Collections.sort(possibilityObjectives, Comparator.comparing(po -> po.getTotalDistance()));

        while (possibilityObjectives.size() > 0) {
            PossibilityObjective actualObjectif = possibilityObjectives.get(0);

            List<PossibilityObjective> actualObjectives = new ArrayList<PossibilityObjective>();
            actualObjectives.addAll(possibilityObjectives.stream()
                    .filter(po -> po.getTotalDistance() == actualObjectif.getTotalDistance()
                            && po.getObjectiveList().contains(actualObjectif.getObjectiveList().get(0))
                            && po.getObjectiveList().size() == 1)
                    .collect(Collectors.toList()));

            if (actualObjectives.size() == 0) {
                actualObjectives.addAll(possibilityObjectives.stream()
                        .filter(po -> po.getTotalDistance() == actualObjectif.getTotalDistance()
                                && po.getObjectiveList().contains(actualObjectif.getObjectiveList().get(0)))
                        .collect(Collectors.toList()));
            }

            List<Pac> actualPacs = new ArrayList<Pac>();
            actualObjectives.forEach(po -> actualPacs.add(po.getPac()));

            Pac chosenPac = null;

            for (int i = 0; i < actualObjectives.size(); i++) {
                Pac loopPac = actualObjectives.get(i).getPac();

                if (loopPac.isMine() && !actualPacs.stream()
                        .anyMatch(p -> !p.isMine() && p.getTypeId().equals(loopPac.getCounter(loopPac.getTypeId())))) {
                    chosenPac = loopPac;
                    actualObjectives.get(i).getObjectiveList().forEach(c -> loopPac.addObligatoryObjective(c));
                    break;
                }
            }

            possibilityObjectives.removeAll(possibilityObjectives.stream()
                    .filter(po -> po.getObjectiveList().contains(actualObjectif.getObjectiveList().get(0)))
                    .collect(Collectors.toList()));

            if (actualObjectif.getObjectiveList().size() == 2) {
                possibilityObjectives.removeAll(possibilityObjectives.stream()
                        .filter(po -> po.getObjectiveList().contains(actualObjectif.getObjectiveList().get(1)))
                        .collect(Collectors.toList()));

            }

            if (chosenPac != null) {
                Pac pacToDelete = chosenPac;
                possibilityObjectives.removeAll(possibilityObjectives.stream()
                        .filter(po -> po.getPac().equals(pacToDelete)).collect(Collectors.toList()));
            }
        }

        pacs.stream()
                .filter(p -> p.isMine() && !p.getCurrentCell().isIntersection()
                        && p.getCurrentCell().getPipe().isDeadEnd() && p.getObligatoryObjectives().isEmpty())
                .forEach(p -> p
                        .addObligatoryObjective(p.getCurrentCell().getPipe().getIntersections().get(0).getCell()));

    }

    public static void analyzeBigPipes() {
        List<Pipe> bigPipes = pipes.stream()
                .filter(p -> p.getIntersections().size() > 1 && p.getPointsCells() >= 5 && !pacs.stream()
                        .anyMatch(pac -> pac.getObligatoryObjectives().size() > 0
                                && pac.getObligatoryObjectives().contains(p.getCells().get(p.getCells().size() / 2))))
                .collect(Collectors.toList());

        Collections.sort(bigPipes, Comparator.comparing(p -> p.getPointsCells()));
        Collections.reverse(bigPipes);

        pacs.stream().filter(p -> p.getObligatoryObjectives().size() == 0).forEach(p -> {
            if (bigPipes.size() > 0) {
                Collections.sort(bigPipes, new Comparator() {
                    public int compare(Object o1, Object o2) {

                        Integer ca1 = ((Pipe) o1).getCells().get(((Pipe) o1).getCells().size() / 2)
                                .getDistanceTo(p.getCurrentCell());
                        Integer ca2 = ((Pipe) o2).getCells().get(((Pipe) o2).getCells().size() / 2)
                                .getDistanceTo(p.getCurrentCell());
                        int resultComp = ca1.compareTo(ca2);

                        if (resultComp != 0) {
                            return resultComp;
                        }

                        Integer cb1 = ((Pipe) o1).getPointsCells();
                        Integer cb2 = ((Pipe) o2).getPointsCells();
                        return -cb1.compareTo(cb2);
                    }
                });

                p.addObligatoryObjective(bigPipes.get(0).getCells().get(bigPipes.get(0).getCells().size() / 2));
            }
        });
    }
}

class Pac {
    private int id;
    private Cell currentCell;
    private boolean mine;
    private List<Cell> targetedBigPellets;
    private List<Cell> obligatoryObjectives;
    private String action;
    private String typeId;
    private int speedTurnsLeft;
    private int abilityCooldown;
    private int speed;
    private List<Cell> previousCells;
    private boolean isInDanger;
    private int counterFlee;
    private Cell lastEnemyPosition;

    public Pac(int id, int x, int y, boolean mine, String typeId) {
        this.id = id;
        this.mine = mine;
        this.previousCells = new ArrayList<Cell>();
        this.targetedBigPellets = new ArrayList<Cell>();
        this.obligatoryObjectives = new ArrayList<Cell>();
        this.action = "";
        this.typeId = typeId;
        this.speed = 1;
        setNewCell(x, y);
        this.currentCell.setPoints(0);
        this.isInDanger = false;
        this.counterFlee = 0;
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
        if (mine) {
            this.currentCell.setPacOnCell(null);
        }

        Player.pipes.stream().filter(p -> this.equals(p.isTakenBy())).findAny().ifPresent(p -> p.setIsTakenBy(null));
        if (this.currentCell.getIntersection() != null) {
            this.currentCell.getIntersection().setIsTaken(false);
        }
    }

    public List<Cell> getTargetedBigPellets() {
        return this.targetedBigPellets;
    }

    public void addTargetedBigPellet(Cell target) {
        this.targetedBigPellets.add(target);
    }

    public List<Cell> getObligatoryObjectives() {
        return this.obligatoryObjectives;
    }

    public void addObligatoryObjective(Cell target) {
        this.obligatoryObjectives.add(target);
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
        this.typeId = typeId;
        setNewCell(x, y);
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
        this.currentCell.getVisionCells().stream().forEach(c -> c.setPoints(0));
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

                    if (pipe.getIntersections().size() > 1 && !this.currentCell.getNextCells().contains(c)) {
                        intersection = pipe.getIntersections().get(1);
                    }

                    if (this.currentCell.getNextCells().contains(c)) {
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
        if (this.previousCells.size() > 15) {
            this.previousCells.remove(0);
        }

        cell.setPoints(0);
        this.currentCell = cell;
        if (!this.typeId.equals("DEAD")) {
            cell.setPacOnCell(this);
        }

        this.targetedBigPellets.removeIf(c -> cell.equals(c));
        this.obligatoryObjectives.removeIf(c -> cell.equals(c));
    }

    public void disappear() {
        this.currentCell.setPacOnCell(null);
        this.currentCell = null;
        this.targetedBigPellets.removeAll(this.targetedBigPellets);
        this.obligatoryObjectives.removeAll(this.obligatoryObjectives);
    }

    public DistancePac searchClosestAlliedPacFlightDistance() {
        List<Pac> alliesPacs = Player.pacs.stream().filter(p -> p.isMine() && !p.equals(this))
                .collect(Collectors.toList());

        if (alliesPacs.size() == 0) {
            return null;
        }

        List<DistancePac> distPac = new ArrayList<DistancePac>();

        alliesPacs.forEach(p -> {
            Double distance = this.currentCell.calculFlightDistance(p.getCurrentCell());
            distPac.add(new DistancePac(distance, p));
        });

        return Collections.min(distPac, Comparator.comparing(d -> d.getDistance()));
    }

    public Cell getClosestPelletCell() {
        if (this.obligatoryObjectives.size() > 0) {
            Cell objective = this.obligatoryObjectives.get(0);
            if (this.speed == 2 && this.currentCell.getNextCells().contains(objective)) {

                List<Cell> newCandidatsCells = new ArrayList<Cell>();

                newCandidatsCells.addAll(objective.getNextCells().stream()
                        .filter(c -> !this.currentCell.equals(c) && c.getPoints() > 0
                                && ((c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                                        && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null))))
                        .collect(Collectors.toList()));

                if (newCandidatsCells.size() == 1) {
                    objective = newCandidatsCells.get(0);
                } else if (newCandidatsCells.size() > 0) {
                    Cell cellToTarget = newCandidatsCells.stream()
                            .filter(c -> c.isIntersection() || !c.getPipe().isDeadEnd()).findAny().orElse(null);
                    if (cellToTarget != null) {
                        objective = cellToTarget;
                    } else {
                        objective = newCandidatsCells.get(0);
                    }
                }
            }

            return objective;

        } else if (this.targetedBigPellets.size() > 0) {
            Cell objective = this.targetedBigPellets.get(0);
            if (this.speed == 2 && this.currentCell.getNextCells().contains(objective)) {

                List<Cell> newCandidatsCells = new ArrayList<Cell>();

                newCandidatsCells.addAll(objective.getNextCells().stream()
                        .filter(c -> !this.currentCell.equals(c) && c.getPoints() > 0
                                && ((c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                                        && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null))))
                        .collect(Collectors.toList()));

                if (newCandidatsCells.size() == 1) {
                    objective = newCandidatsCells.get(0);
                } else if (newCandidatsCells.size() > 0) {
                    Cell cellToTarget = newCandidatsCells.stream()
                            .filter(c -> c.isIntersection() || !c.getPipe().isDeadEnd()).findAny().orElse(null);
                    if (cellToTarget != null) {
                        objective = cellToTarget;
                    } else {
                        objective = newCandidatsCells.get(0);
                    }
                }
            }

            return objective;
        }

        DistancePac closestAllyPac = searchClosestAlliedPacFlightDistance();

        Cell closestTarget = null;
        long counter = 1L;

        List<Cell> alreadySeenCells = new ArrayList<Cell>();
        alreadySeenCells.add(this.currentCell);

        List<Cell> actualCells = this.currentCell.getNextCells().stream()
                .filter(c -> (c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                        && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null)))
                .collect(Collectors.toList());

        List<Cell> candidatsCells = new ArrayList<Cell>();

        do {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPoints() > 0
                        && ((Player.remainingPallets < Player.consumedPallets && (closestAllyPac == null
                                || closestAllyPac.getDistance() < actualCell.calculFlightDistance(this.currentCell)))
                                || actualCell.searchClosestAlliedPac().equals(this))) {
                    candidatsCells.add(actualCell);
                } else if (actualCell.getPoints() > 0 && closestTarget == null) {
                    closestTarget = actualCell;
                }

                futurCells.addAll(actualCell.getNextCells().stream()
                        .filter(c -> !alreadySeenCells.contains(c) && !futurCells.contains(c)
                                && ((c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                                        && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null))))
                        .collect(Collectors.toList()));
                counter++;
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;

        } while (actualCells.size() > 0 && candidatsCells.size() == 0 && counter < Player.limitCalculations);

        if (candidatsCells.size() == 1) {
            closestTarget = candidatsCells.get(0);
        } else if (candidatsCells.size() > 0) {
            List<Cell> sortedCandidats = candidatsCells
                    .stream().filter(c -> this.getObligatoryObjectives().isEmpty()
                            && this.getTargetedBigPellets().isEmpty() && c.isIntersection() || !c.getPipe().isDeadEnd())
                    .collect(Collectors.toList());

            if (closestAllyPac != null) {
                Collections.sort(sortedCandidats,
                        Comparator.comparing(c -> c.calculFlightDistance(closestAllyPac.getPac().getCurrentCell())));
                Collections.reverse(sortedCandidats);
            }

            if (sortedCandidats.size() > 0) {
                closestTarget = sortedCandidats.get(0);
            } else {
                closestTarget = candidatsCells.get(0);
            }
        }

        if (this.speed == 2 && closestTarget != null) {
            List<Cell> newCandidatsCells = new ArrayList<Cell>();
            Cell finalClosestTarget = closestTarget;

            newCandidatsCells.addAll(finalClosestTarget.getNextCells().stream()
                    .filter(c -> c.getPacOnCell() == null && !alreadySeenCells.contains(c) && !newCandidatsCells.contains(c) && c.getPoints() > 0
                            && ((c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                                    && (c.getPipe().isTakenBy() == this || c.getPipe().isTakenBy() == null))))
                    .collect(Collectors.toList()));

            if (newCandidatsCells.size() == 1) {
                closestTarget = newCandidatsCells.get(0);
            } else if (newCandidatsCells.size() > 0) {
                Cell cellToTarget = newCandidatsCells.stream()
                        .filter(c -> c.isIntersection() || !c.getPipe().isDeadEnd()).findAny().orElse(null);
                if (cellToTarget != null) {
                    closestTarget = cellToTarget;
                } else {
                    closestTarget = newCandidatsCells.get(0);
                }
            }
        }

        if (closestTarget == null) {
            closestTarget = this.currentCell.getOpposedCell(this);
        }

        return closestTarget;
    }

    public void nextPriorityAction() {
        String myCounter = getCounter(this.typeId);
        String counterOfMyCounter = getCounter(myCounter);

        if (counterFlee > 0) {
            counterFlee--;
            flee(lastEnemyPosition, this.currentCell.getDistanceTo(lastEnemyPosition));

        } else if (Player.round != 1 && this.abilityCooldown == 0 && !this.isInDanger && Player.pacs.stream()
                .filter(p -> !p.isMine() && p.getTypeId().equals(counterOfMyCounter)).count() == 0) {
            switchTo(myCounter);

        } else if (this.abilityCooldown == 0 && !this.isInDanger) {
            if (this.previousCells.size() >= 3
                    && this.previousCells.subList(this.previousCells.size() - 2, this.previousCells.size()).stream()
                            .filter(c -> c != this.previousCells.get(this.previousCells.size() - 3)).count() == 0) {

                if (this.previousCells.size() >= 13 && this.previousCells
                        .subList(this.previousCells.size() - 12, this.previousCells.size()).stream()
                        .filter(c -> c != this.previousCells.get(this.previousCells.size() - 13)).count() == 0) {
                    switchTo(getCounter(getCounter(this.typeId)));
                } else {
                    switchTo(getCounter(this.typeId));
                }

            } else {
                activateSpeed();
            }
        }
    }

    public void nextAction() {
        if (!currentCell.isIntersection() && currentCell.getPipe().isDeadEnd()
                && currentCell.getPipe().getPointsCells() == 0) {
            moveTo(currentCell.getPipe().getIntersections().get(0).getCell());

        } else {
            moveTo(this.getClosestPelletCell());
        }
    }

    public void refreshBigTargets() {
        this.obligatoryObjectives.removeIf(tbp -> tbp.getPoints() < 1);
        this.targetedBigPellets.removeIf(tbp -> tbp.getPoints() < 1);
    }

    public void actionOnEnnemy(Pac ennemyPac) {
        if (this.targetedBigPellets.isEmpty() && this.obligatoryObjectives.isEmpty()) {
            int distance = this.currentCell.getDistanceTo(ennemyPac.getCurrentCell());
            boolean doPacWin = doPacWin(ennemyPac);
            String ennemyCounter = getCounter(ennemyPac.getTypeId());

            if (!doPacWin && distance <= ennemyPac.speed
                    && (!ennemyPac.getTypeId().equals(this.typeId) || ennemyPac.getTypeId().equals(this.typeId)
                            && ennemyPac.getAbilityCooldown() <= this.abilityCooldown)) {

                if (Player.round != 1 && distance <= ennemyPac.getSpeed()) {
                    this.isInDanger = true;
                    this.counterFlee = 5 - distance;
                    flee(ennemyPac.getCurrentCell(), distance);
                } else if (this.abilityCooldown == 0) {
                    activateSpeed();
                }
            }

            if (Player.round != 1 && (ennemyPac.getSpeed() >= distance) && !doPacWin && this.abilityCooldown == 0) {
                switchTo(ennemyCounter);
            }

            if (Player.round != 1 && !doPacWin && ennemyPac.getAbilityCooldown() > 0 && this.abilityCooldown == 0) {
                this.print();
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

    public void flee(Cell ennemyCell, int distance) {
        this.lastEnemyPosition = ennemyCell;

        Cell fleeCell = this.currentCell.searchFleeCell(this, ennemyCell, this.speed, distance);
        if (fleeCell != null)
            moveTo(fleeCell);
    }

    public void print() {
        System.err.println("Pac " + (mine ? "ally " : "ennemy ") + this.id + " " + this.typeId);
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
    private List<Cell> visionCells;
    private List<Cell> nextCells;

    public Cell(int x, int y, int points) {
        this.x = x;
        this.y = y;
        this.points = points;
        this.isIntersection = false;
        this.pacOnCell = null;
        this.visionCells = new ArrayList<Cell>();
        this.nextCells = new ArrayList<Cell>();
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

    public List<Cell> getVisionCells() {
        return visionCells;
    }

    public List<Cell> getNextCells() {
        return nextCells;
    }

    public void addNextCell(Cell nextCell) {
        this.nextCells.add(nextCell);
    }

    public void addNextCells(List<Cell> nextCells) {
        this.nextCells.addAll(nextCells);
    }

    public void addVisionCells(List<Cell> newVisionCells) {

        this.visionCells.addAll(
                newVisionCells.stream().filter(c -> !this.visionCells.contains(c)).collect(Collectors.toList()));
    }

    public double calculFlightDistance(Cell cell) {
        return Double.valueOf(Math.sqrt(Math.pow(this.x - cell.getX(), 2) + Math.pow(this.y - cell.getY(), 2)));
    }

    public Cell getOpposedCell(Pac pac) {
        List<Cell> copyCells = new ArrayList<Cell>();
        copyCells.addAll(Player.cells.stream()
                .filter(c -> (c.isIntersection && !c.intersection.isTaken())
                        || (!c.isIntersection && (c.pipe.isTakenBy() == null || c.pipe.isTakenBy() == pac)))
                .collect(Collectors.toList()));

        Collections.sort(copyCells, Comparator.comparing(c -> c.calculFlightDistance(this)));

        Cell result = copyCells.get(copyCells.size() - 1);

        return result;
    }

    public Pac searchClosestPacFlightDistance() {
        List<DistancePac> distPac = new ArrayList<DistancePac>();

        Player.pacs.stream().filter(p -> p.getCurrentCell() != null).forEach(p -> {
            Double distance = this.calculFlightDistance(p.getCurrentCell());
            distPac.add(new DistancePac(distance, p));
        });

        return Collections.min(distPac, Comparator.comparing(d -> d.getDistance())).getPac();
    }

    public Pac searchClosestAllyPacFlightDistance() {
        List<Pac> alliesPacs = Player.pacs.stream().filter(p -> p.isMine()).collect(Collectors.toList());

        if (alliesPacs.size() == 0) {
            return null;
        }

        List<DistancePac> distPac = new ArrayList<DistancePac>();

        alliesPacs.forEach(p -> {
            Double distance = this.calculFlightDistance(p.getCurrentCell());
            distPac.add(new DistancePac(distance, p));
        });

        return Collections.min(distPac, Comparator.comparing(d -> d.getDistance())).getPac();
    }

    public Pac searchClosestPac() {
        long counter = 1L;

        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = new ArrayList<Cell>();
        actualCells.addAll(this.nextCells);

        do {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPacOnCell() != null) {
                    return actualCell.getPacOnCell();
                }

                futurCells.addAll(actualCell.getNextCells().stream()
                        .filter(c -> !alreadySeenCells.contains(c) && !futurCells.contains(c))
                        .collect(Collectors.toList()));
                counter++;
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
        } while (actualCells.size() > 0 && counter < Player.limitCalculations);

        return searchClosestPacFlightDistance();
    }

    public Pac searchClosestAlliedPac() {
        long counter = 1L;

        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = new ArrayList<Cell>();
        actualCells.addAll(this.nextCells);

        do {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPacOnCell() != null && actualCell.getPacOnCell().isMine()) {
                    return actualCell.getPacOnCell();
                }

                futurCells.addAll(actualCell.getNextCells().stream()
                        .filter(c -> !alreadySeenCells.contains(c) && !futurCells.contains(c))
                        .collect(Collectors.toList()));
                counter++;
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
        } while (actualCells.size() > 0 && counter < Player.limitCalculations);

        return searchClosestAllyPacFlightDistance();
    }

    public Cell searchFleeCell(Pac allyPac, Cell ennemyCell, int speed, int distance) {
        List<Cell> possibleCells = this.getNextCells().stream()
                .filter(c -> c.getPacOnCell() == null
                        && ((c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                                && (c.getPipe().isTakenBy() == allyPac || c.getPipe().isTakenBy() == null))))
                .collect(Collectors.toList());

        if (speed == 2) {
            List<Cell> newCells = new ArrayList<Cell>();
            List<Cell> tempCells = possibleCells;

            for (int i = 0; i < possibleCells.size(); i++) {
                Cell actualCell = possibleCells.get(i);

                newCells.addAll(actualCell.getNextCells().stream()
                        .filter(c -> c.getPacOnCell() == null && !tempCells.contains(c) && !newCells.contains(c)
                                && ((c.isIntersection() && !c.getIntersection().isTaken()) || (!c.isIntersection()
                                        && (c.getPipe().isTakenBy() == allyPac || c.getPipe().isTakenBy() == null))))
                        .collect(Collectors.toList()));
            }

            possibleCells = newCells;
        }

        possibleCells = possibleCells.stream().filter(c -> ennemyCell.getDistanceTo(c) > distance)
                .collect(Collectors.toList());

        Cell result = possibleCells.stream()
                .filter(c -> (c.isIntersection || !c.getPipe().isDeadEnd()) && c.getPoints() > 0).findAny()
                .orElse(null);

        if (result == null) {
            result = possibleCells.stream().filter(c -> c.isIntersection || !c.getPipe().isDeadEnd()).findAny()
                    .orElse(null);
        }

        if (result == null) {
            List<Cell> deadEndCells = possibleCells.stream().filter(c -> !c.isIntersection)
                    .collect(Collectors.toList());

            if (deadEndCells.size() > 0) {
                Collections.sort(deadEndCells, Comparator.comparing(c -> c.getPipe().getPointsCells()));
                result = deadEndCells.get(deadEndCells.size() - 1);
            }
        }

        if (result != null) {
            return result;
        }

        return possibleCells.size() > 0 ? possibleCells.get(0) : null;
    }

    public int getDistanceTo(Cell cell) {
        int distance = 1;
        boolean found = false;
        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = new ArrayList<Cell>();
        actualCells.addAll(this.nextCells);

        while (true) {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.equals(cell)) {
                    found = true;
                    break;
                }

                futurCells.addAll(actualCell.getNextCells().stream()
                        .filter(c -> !alreadySeenCells.contains(c) && !futurCells.contains(c))
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
                + (this.pacOnCell != null ? this.pacOnCell.isMine() + " " + this.pacOnCell.getId() : "null")
                + " istaken=" + (isIntersection ? intersection.isTaken()
                        : (pipe.isTakenBy() != null ? pipe.isTakenBy().getId() : null)));
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

class DistancePac {
    private Double distance;
    private Pac pac;

    public DistancePac(Double distance, Pac pac) {
        this.distance = distance;
        this.pac = pac;
    }

    public Double getDistance() {
        return distance;
    }

    public Pac getPac() {
        return pac;
    }
}

class DistancePacBigPellet {
    private int distance;
    private Pac pac;
    private Cell cell;

    public DistancePacBigPellet(int distance, Pac pac, Cell cell) {
        this.distance = distance;
        this.pac = pac;
        this.cell = cell;
    }

    public int getDistance() {
        return distance;
    }

    public Pac getPac() {
        return pac;
    }

    public Cell getCell() {
        return cell;
    }
}

class DistanceBetweenBigPellets {
    private int distance;
    private List<Cell> cells;

    public DistanceBetweenBigPellets(int distance, Cell cell1, Cell cell2) {
        this.distance = distance;
        this.cells = new ArrayList<Cell>();
        this.cells.add(cell1);
        this.cells.add(cell2);
    }

    public int getDistance() {
        return distance;
    }

    public boolean containsCells(Cell cell1, Cell cell2) {
        if (this.cells.contains(cell1) && this.cells.contains(cell2)) {
            return true;
        }

        return false;
    }
}

class PossibilityObjective {
    private List<Cell> objectiveList;
    private int totalDistance;
    private Pac pac;

    public PossibilityObjective(Cell cell, int totalDistance, Pac pac) {
        this.totalDistance = totalDistance;
        this.pac = pac;

        this.objectiveList = new ArrayList<Cell>();
        this.objectiveList.add(cell);
    }

    public PossibilityObjective(Cell cell1, Cell cell2, int totalDistance, Pac pac) {
        this.totalDistance = totalDistance;
        this.pac = pac;

        this.objectiveList = new ArrayList<Cell>();
        this.objectiveList.add(cell1);
        this.objectiveList.add(cell2);
    }

    public List<Cell> getObjectiveList() {
        return objectiveList;
    }

    public Pac getPac() {
        return pac;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

}