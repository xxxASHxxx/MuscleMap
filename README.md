# MuscleMap 🏋️‍♂️

A powerful fitness tracker and exercise library application built with JavaFX that combines the best features of popular fitness apps like Hevy and MuscleWiki. Perfect for anyone who wants to track their workouts while learning proper exercise techniques.

## About The Project

MuscleMap was born out of the need for a comprehensive fitness application that doesn't just track your workouts, but also educates you about exercises and muscle anatomy. Whether you're a beginner trying to understand which exercises target which muscles, or an experienced lifter looking for detailed workout logging, MuscleMap has you covered.

### What Makes It Special

- **Interactive Muscle Mapping** - Click on muscle groups to discover targeted exercises
- **Comprehensive Exercise Database** - Detailed exercise library with proper form instructions  
- **Smart Workout Tracking** - Log sets, reps, and weights with an intuitive interface
- **Progress Analytics** - Visualize your strength gains over time
- **Offline First** - All data stored locally, no internet required
- **Clean UI Design** - Modern JavaFX interface with smooth animations

## Built With

- **Java 21** - Latest LTS version for optimal performance
- **JavaFX 21** - Modern desktop UI framework
- **SQLite** - Lightweight database for local storage
- **Maven** - Dependency management and build automation
- **Jackson** - JSON processing for data handling

## Getting Started

### What You'll Need

Before running MuscleMap, make sure you have:
- Java 21 or higher installed
- Maven 3.6+ (optional - we include Maven wrapper)
- About 100MB of free disk space

### Quick Start

1. **Get the code**
git clone https://github.com/xxxASHxxx/MuscleMap.git
cd MuscleMap

2. **Run the application**
On Linux/Mac
./mvnw clean javafx:run

On Windows
mvnw.cmd clean javafx:run


3. **Start tracking!**
- The app will create its database automatically
- Explore the exercise library
- Start logging your first workout

### Alternative Installation

If you prefer building a standalone version:

./mvnw clean compile package
java -jar target/muscle-map-app-1.0.0.jar

## How To Use

### First Time Setup
The app works right out of the box. On first launch, it automatically creates the SQLite database and sets up everything you need to start tracking your fitness journey.

### Logging Workouts
1. Navigate to the workout section
2. Select exercises from the comprehensive library
3. Add your sets, reps, and weights as you complete them
4. Save your workout to track progress over time

### Exploring Exercises
- Use the interactive muscle map to find exercises for specific body parts
- Browse the complete exercise database with filtering options
- View detailed instructions and form tips for each exercise

### Tracking Progress
- Review your workout history and spot trends
- See strength improvements across different exercises
- Analyze your training consistency and volume

## Development Story

This project started as part of my computer science coursework at SRM Institute, but it quickly became something more personal. As someone passionate about fitness and programming, I wanted to create an app that I'd actually use for my own workouts.

The development process involved learning advanced JavaFX concepts, working with databases, and solving real UI/UX challenges. Every feature was built with the user experience in mind, making sure the app feels natural and intuitive.

## Contributing

While this is primarily a personal project, I'm open to suggestions and improvements! Here's how you can contribute:

### Reporting Issues
- Found a bug? Open an issue with detailed steps to reproduce
- Have a feature idea? Share it in the issues section
- UI/UX feedback is always appreciated

### Code Contributions
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Roadmap

### Coming Soon
- [ ] Exercise video demonstrations
- [ ] Custom workout plan templates  
- [ ] Data export functionality
- [ ] Advanced progress charts and analytics
- [ ] Exercise timer and rest periods

### Future Ideas
- [ ] Nutrition tracking integration
- [ ] Social features for sharing workouts
- [ ] Mobile companion app
- [ ] Cloud sync capabilities
- [ ] Exercise form analysis using AI

## Technical Details

### Database Schema
The app uses SQLite for data persistence with tables for:
- Exercise library and muscle group mappings
- Workout sessions and exercise logs
- User preferences and settings
- Progress tracking data

### Performance
- Optimized for smooth 60fps animations
- Efficient database queries with proper indexing
- Lazy loading for large datasets
- Minimal memory footprint

## License

This project is open source and available under the MIT License. Feel free to use it for educational purposes or as inspiration for your own fitness applications.

## Acknowledgments

- Thanks to the JavaFX community for excellent documentation and examples
- Inspired by the simplicity of Hevy and the educational approach of MuscleWiki
- Built with love for the fitness community and fellow developers

---

**Made with ❤️ and lots of coffee** ☕

*"The best fitness app is the one you'll actually use."* That's the philosophy behind MuscleMap - simple, powerful, and built for real workouts.

---



