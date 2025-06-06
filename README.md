# Fluid Simulation

The purpose of this project is to create a particle based fluid simulation in java with swing. I was inspired by my discovery of Antz being the first practical use of a fluid simulation that was not animated by hand. Animators were given two tools to create their worls, a hose like structure that produced the water and ways to define solid surfaces for the water to interact with. Because of this, in my java code, I want to be able generate particles with a click and define a solid cube. 

## Main Ideas

I want this to be created in java so it can be compiled and run wihtout external libraries or dependencies and even turned into executable. 

Based on the papers I read, the particles in the simulation, AKA Smoother Particle Hydrodynamics(SPH), have five main attributes: velocity, acceleration, density, pressure, and the location. The calculate these properties, we only use the "quantities in a local neighborhood of each particle using radial symmetrical smoothing kernels". This circle is defined by us and helps limit the computations made. 

## Running the tests

Explain how to run the automated tests for this system

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Acknowledgments

https://www.sci.utah.edu/publications/premoze03/ParticleFluidsHiRes.pdf
https://matthias-research.github.io/pages/publications/sca03.pdf
