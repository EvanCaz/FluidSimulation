# Fluid Simulation

The purpose of this project is to create a particle based fluid simulation in java with swing. I was inspired by my discovery of Antz being the first practical use of a fluid simulation that was not animated by hand. Animators were given two tools to create their worls, a hose like structure that produced the water and ways to define solid surfaces for the water to interact with. Because of this, in my java code, I want to be able generate particles with a click and define a solid cube. 

## Main Ideas

I want this to be created in java so it can be compiled and run wihtout external libraries or dependencies and even turned into executable. 

Based on the papers I read, the particles in the simulation, AKA Smoother Particle Hydrodynamics(SPH), have five main attributes: velocity, acceleration, density, pressure, and the location. The calculate these properties, we only use the "quantities in a local neighborhood of each particle using radial symmetrical smoothing kernels". 

Radius is defined by us and helps limit the area of interaction(so only the particles in this area effect density and forces) made as well as the spatial map for checking the closest 8 plus current grid for calculations not the entire window. 
Rest density is the target density where no preassure is created, smaller means particles sit farther apart. 
Gas Constant or sitffness controlls preassure scalling, large makes huge preassure forces aka incompressible, small does opposite. 
Viscocity dampens interactions, higher means a "thicker" simulation. 
Restituiton controls how particles interact with boundaries.

## Example

<video src='your URL here' width=180/>

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
https://docs.oracle.com/javase/tutorial/uiswing/components/slider.html
