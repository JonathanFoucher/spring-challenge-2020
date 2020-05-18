// retirer les collect size => count()
// retirer collect foreach

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

    public static List<Intersection> intersections = new ArrayList<Intersection>();
    public static List<Pipe> pipes = new ArrayList<Pipe>();

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
                actualCell.setIsIntersection(true);
                intersections.add(new Intersection(actualCell));
            }
        }

        // intersections.stream().forEach(i -> System.err.println(i.getCell().getX() + "
        // " + i.getCell().getY()));

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
                } else {
                    nextIntersection = pipe.getIntersections().stream().filter(inter -> inter != intersection).findAny()
                            .orElse(null);
                }

                intersection.addPathsMap(pipe, nextIntersection);
            }
        }

        // intersections.stream().filter(inter -> inter.getCell().isPosition(31,
        // 7)).forEach(intersection -> intersection.getPathsMap()
        // .forEach((pipe,interTarget) -> System.err.println("inter x=" +
        // intersection.getCell().getX()+ " y=" + intersection.getCell().getY() + "
        // pipe1 x=" +pipe.getCells().get(0).getX() + " y=" +
        // pipe.getCells().get(0).getY() +" pipe2 x=" +
        // pipe.getCells().get(pipe.getCells().size() - 1).getX() +" y=" +
        // pipe.getCells().get(pipe.getCells().size() - 1).getY() +" interTarget x=" +
        // (interTarget == null ? null :interTarget.getCell().getX()) + " y=" +
        // (interTarget == null ? null :interTarget.getCell().getY()))));

        // cells.forEach(c -> System.err.println("x:" + c.getX() + " " + "y:" + c.getY()
        // + " "));

        int myScore = in.nextInt();
        int opponentScore = in.nextInt();

        int visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
        for (int i = 0; i < visiblePacCount; i++) {
            int pacId = in.nextInt(); // pac number (unique within a team)
            boolean mine = in.nextInt() != 0; // true if this pac is yours
            int x = in.nextInt(); // position in the grid
            int y = in.nextInt(); // position in the grid
            String typeId = in.next(); // unused in wood leagues
            int speedTurnsLeft = in.nextInt(); // unused in wood leagues
            int abilityCooldown = in.nextInt(); // unused in wood leagues

            pacs.add(new Pac(pacId, x, y, mine));
        }

        int visiblePelletCount = in.nextInt(); // all pellets in sight
        for (int i = 0; i < visiblePelletCount; i++) {
            int x = in.nextInt();
            int y = in.nextInt();
            int value = in.nextInt(); // amount of points this pellet is worth
            // System.err.println("x:" + x + " " + "y:" + y + " " );

            Cell actualCell = cells.stream().filter(c -> c.getX() == x && c.getY() == y).findAny().get();
            actualCell.setPoints(value);
            if(value > 1) {
                actualCell.searchClosestPac().addTargetedBigPellets(actualCell);
            }
        }

        // PRINT CLOSEST BIG PELLETS TARGETED BY PACS
        // pacs.stream().forEach(p -> p.getTargetedBigPellets().forEach(bp -> System.err.println(p.getCurrentCell().getX() + " " + p.getCurrentCell().getY() + "   " + bp.getX() + " " + bp.getY())));

        pacs.stream().filter(c -> c.isMine()).collect(Collectors.toList())
                .forEach(p -> p.moveTo(p.getClosestPelletCell()));

        // game loop
        while (true) {
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

                pacs.stream().filter(p -> p.getId() == pacId && p.isMine() == mine).findAny().get().setNewCell(x, y);
            }

            visiblePelletCount = in.nextInt(); // all pellets in sight
            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth
                // System.err.println("x:" + x + " " + "y:" + y + " " );

                cells.stream().filter(c -> c.getX() == x && c.getY() == y).findAny().get().setPoints(value);
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            pacs.stream().filter(c -> c.isMine()).collect(Collectors.toList())
                    .forEach(p -> p.moveTo(p.getClosestPelletCell())); // MOVE <pacId> <x> <y>
        }
    }

    public static Cell getSymetricCell(Cell cell, int width, List<Cell> cells) {
        return cells.stream().filter(c -> c.getX() + 1 == (width - cell.getX()) && c.getY() == cell.getY()).findAny()
                .get();
    }
}

class Pac {
    private int id;
    private Cell currentCell;
    private boolean mine;
    private List<Cell> targetedBigPellets;

    public Pac(int id, int x, int y, boolean mine) {
        this.id = id;
        this.currentCell = Player.cells.stream().filter(c -> c.getX() == x && c.getY() == y).findAny().get();
        this.currentCell.setPoints(0);
        this.mine = mine;
        this.targetedBigPellets = new ArrayList<Cell>();
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

    public List<Cell> getTargetedBigPellets() {
        return this.targetedBigPellets;
    }

    public void addTargetedBigPellets(Cell target) {
        this.targetedBigPellets.add(target);
    }

    public boolean isAtIntersection() {
        return currentCell.isIntersection();
    }

    public void moveTo(Cell cell) {
        System.out.println("MOVE " + id + " " + cell.getX() + " " + cell.getY());
    }

    public void setNewCell(int x, int y) {
        Cell cell = Player.cells.stream().filter(c -> c.getX() == x && c.getY() == y).findAny().get();
        cell.setPoints(0);
        this.currentCell = cell;

        if (this.targetedBigPellets.contains(cell)) {
            this.targetedBigPellets.remove(cell);
        }
    }

    public Cell getClosestPelletCell() {
        if (this.targetedBigPellets.size() > 0) {
            return this.targetedBigPellets.get(0);
        }

        final int x = this.currentCell.getX();
        final int y = this.currentCell.getY();

        List<Cell> alreadySeenCells = new ArrayList<Cell>();

        List<Cell> actualCells = Player.cells.stream().filter(c -> (c.isPosition((x + 1) % Player.width, y)
                || c.isPosition((x - 1 + Player.width) % Player.width, y) || c.isPosition(x, (y + 1) % Player.height)
                || c.isPosition(x, (y - 1 + Player.height) % Player.height))).collect(Collectors.toList());

        do {
            List<Cell> futurCells = new ArrayList<Cell>();

            for (int i = 0; i < actualCells.size(); i++) {
                Cell actualCell = actualCells.get(i);

                if (actualCell.getPoints() > 0) {
                    return actualCell;
                }

                futurCells.addAll(Player.cells.stream()
                        .filter(c -> (c.isPosition((actualCell.getX() + 1) % Player.width, actualCell.getY())
                                || c.isPosition((actualCell.getX() - 1 + Player.width) % Player.width,
                                        actualCell.getY())
                                || c.isPosition(actualCell.getX(), (actualCell.getY() + 1) % Player.height)
                                || c.isPosition(actualCell.getX(),
                                        (actualCell.getY() - 1 + Player.height) % Player.height))
                                && alreadySeenCells.stream()
                                        .filter(ac -> ac.getX() == c.getX() && ac.getY() == c.getY())
                                        .collect(Collectors.toList()).isEmpty()
                                && !futurCells.contains(c))
                        .collect(Collectors.toList()));
            }

            alreadySeenCells.addAll(actualCells);
            actualCells = futurCells;
        } while (actualCells.size() > 0);

        return null;
    }
}

class Cell {
    private int x;
    private int y;
    private int points;
    private boolean targeted;
    private boolean isIntersection;

    public Cell(int x, int y, int points) {
        this.x = x;
        this.y = y;
        this.points = points;
        this.targeted = false;
        this.isIntersection = false;
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

    public boolean isTargeted() {
        return this.targeted;
    }

    public void setTargeted(boolean targeted) {
        this.targeted = targeted;
    }

    public boolean isIntersection() {
        return this.isIntersection;
    }

    public void setIsIntersection(boolean isIntersection) {
        this.isIntersection = isIntersection;
    }

    public Pac searchClosestPac() {
        Map<Pac, Double> distMap = new HashMap<Pac, Double>();
        List<Double> listDist = new ArrayList<Double>();

        Player.pacs.stream().forEach(p -> {
            Double distance = calculDistance(this, p.getCurrentCell());
            listDist.add(distance);
            distMap.put(p, distance);
        });

        return distMap.entrySet().stream().filter(d -> d.getValue() == Collections.min(listDist)).findFirst().get()
                .getKey();

        // !TODO prendre le plus proche sinon le mine sinon celui qui a le moins
        // d'objectif a prendre
    }

    private double calculDistance(Cell cell1, Cell cell2) {
        return Double.valueOf(
                Math.sqrt(Math.pow(cell1.getX() - cell2.getX(), 2) + Math.pow(cell1.getY() - cell2.getY(), 2)));
    }
}

class Intersection {
    Cell cell;
    Map<Pipe, Intersection> pathsMap;

    public Intersection(Cell cell) {
        this.cell = cell;
        this.pathsMap = new HashMap<Pipe, Intersection>();
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
}

class Pipe {
    List<Cell> cells;
    List<Intersection> intersections;

    public Pipe(List<Cell> cells) {
        this.cells = cells;
        this.intersections = new ArrayList<Intersection>();
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
}
