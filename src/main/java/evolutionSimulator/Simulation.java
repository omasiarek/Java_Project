package evolutionSimulator;

import evolutionSimulator.fields.Vector2d;
import evolutionSimulator.map.WorldMap;
import evolutionSimulator.objects.Animal;
import evolutionSimulator.objects.Genotype;
import evolutionSimulator.objects.Plant;

import java.util.*;
import java.util.stream.Collectors;


public class Simulation {
    private final SimulationConfig config;
    private WorldMap map;
    private int counterOfDays;
    private List<Statistic> statistics;
    private List<Integer> lifeTime;

    public Simulation(SimulationConfig config) {
        this.config = config;
        this.map = new WorldMap(config.width, config.height, config.jungleRatio);
        this.counterOfDays = 1;
        this.statistics = new LinkedList<>();
        this.lifeTime = new LinkedList<>();


        for (int i = 0; i < config.initialAnimals; i++) {
            List<Vector2d> freePositions = map.getFreePlaceAtMap();
            Vector2d position = freePositions.get(Generator.GENERATOR.nextInt(freePositions.size()));
            Animal animal = new Animal(this.map, position, config.startEnergy);
            this.map.addElement(animal);
        }
    }

    public void deleteDeathAnimals() {
        List<Animal> animals = map.getAnimals();
        for (Animal animal : animals) {
            if (!animal.isAlive()) {
                lifeTime.add(animal.getAge());
                map.removeElement(animal);
            }
        }
    }

    public void movingAnimal() {
        List<Animal> animals = map.getAnimals();
        for (Animal animal : animals) {
            animal.move(this.config.moveEnergy);
            if(animal.isAlive()){
                animal.addAge();
            }
        }
    }

    public void eating() {
        List<Plant> plants = map.getPlants();
        for (Plant plant : plants) {
            List<Animal> animals = map.getAnimalsPerField(plant.getPosition());
            Collections.sort(animals);
            if (animals.size() != 0) {
                int i = 1;
                while (i<animals.size() && animals.get(i).isAlive() && animals.get(i).getEnergy() == animals.get(0).getEnergy()) {
                    i++;
                }
                if(i==1 && !animals.get(0).isAlive() )
                    continue;
                int energy = this.config.plantEnergy / i;
                for (int j = 0; j < i; j++) {
                    animals.get(j).addEnergy(energy);
                }
                this.map.removeElement(plant);
            }
        }
    }

    public void reproduction() {
        for (Vector2d vector : new ArrayList<>(map.getOccupatedFields())) {
            Vector2d childVector = map.placeForChild(vector);
            if (childVector == null)
                continue;
            List<Animal> animals = map.getAnimalsPerField(vector);
            if (animals.size() < 2)
                continue;
            Collections.sort(animals);
            Animal firstParent = animals.get(0);
            Animal secondParent = animals.get(1);
            int reproductionEnergy = this.config.startEnergy / 2;
            if(firstParent.getEnergy() > reproductionEnergy && secondParent.getEnergy() > reproductionEnergy){
                Animal child = firstParent.reproduction(secondParent, childVector);
                map.addElement(child);
            }
        }
    }

    public void putPlants() {
        List<Vector2d> desert = map.getFreePlaceAtDesert();
        List<Vector2d> jungle = map.getFreePlaceAtJungle();
        if (desert.size() > 0) {
            int index = Generator.GENERATOR.nextInt(desert.size());
            Plant plant = new Plant(desert.get(index));
            map.addElement(plant);
        }
        if (jungle.size() > 0) {
            int index = Generator.GENERATOR.nextInt(jungle.size());
            Plant plant = new Plant(jungle.get(index));
            map.addElement(plant);
        }
    }

    public void oneDay () {
        this.deleteDeathAnimals();
        this.movingAnimal();
        this.eating();
        this.reproduction();
        this.putPlants();
        counterOfDays++;
        this.statistics.add(this.dailyStatistic());
    }

    public Statistic dailyStatistic() {
        int animals = map.getAnimals().size();
        int plants = map.getPlants().size();
        int avgCurrentEnergy = (int) Math.round(
                map.getAnimals().stream()
                    .collect(Collectors.averagingInt(animal -> animal.getEnergy()))
        );
        int children = (int) Math.round(
                map.getAnimals().stream()
                        .collect(Collectors.averagingInt(animal -> animal.getChildren()))
        );
        int avgLifeTime = (int) Math.round(
                this.lifeTime.stream()
                        .collect(Collectors.averagingInt(lifetime -> lifetime))
        );
        Genotype genotype = map.getAnimals().stream()
                .map(animal -> animal.getGenotype())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(genotypeWithCount -> genotypeWithCount.getKey())
                .orElse(null);
        return new Statistic(animals, plants, genotype, avgLifeTime, avgCurrentEnergy, children);
    }

    public String exportStatistics() {
        if (this.statistics.isEmpty()) return "";

        int avgAnimals = (int) Math.round(
                this.statistics.stream()
                        .collect(Collectors.averagingInt(statistic -> statistic.counterAnimals))
        );
        int avgPlants = (int) Math.round(
                this.statistics.stream()
                        .collect(Collectors.averagingInt(statistic -> statistic.counterPlants))
        );
        int avgLifeTime = (int) Math.round(
                this.statistics.stream()
                        .collect(Collectors.averagingInt(statistic -> statistic.avgLifeTime))
        );
        int avgEnergy = (int) Math.round(
                this.statistics.stream()
                        .collect(Collectors.averagingInt(statistic -> statistic.avgCurrentEnergy))
        );
        int avgChildren = (int) Math.round(
                this.statistics.stream()
                        .collect(Collectors.averagingInt(statistic -> statistic.avgChildren))
        );
        Genotype genotype = this.statistics.get(this.statistics.size() - 1).genotype;
        return String.format(
                "Total days: %d\nAverage animals: %d\nAverage plants: %d\nAverage lifetime: %d\nAverage energy: %d\nAverage children: %d\nFinal dominant genome: %s\n",
                this.counterOfDays,
                avgAnimals,
                avgPlants,
                avgLifeTime,
                avgEnergy,
                avgChildren,
                genotype
        );
    }

    public WorldMap getMap() {
        return map;
    }

    public int getDay() {
        return this.counterOfDays;
    }
}
