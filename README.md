# Fluid Simulation

The purpose of this project is to create a particle-based fluid simulation in Java with Swing. I was inspired by my discovery of Antz being the first practical use of a fluid simulation that was not animated by hand. Animators were given two tools to create their worlds: a hose-like structure that produced the water and ways to define solid surfaces for the water to interact with. Because of this, in my Java code, I want to be able to generate particles and move them around. 

## Main Ideas

I want this to be created in Java so it can be compiled and run without external dependencies and even turned into an executable. 

Based on the papers I read, the simulation uses Smoothed Particle Hydrodynamics (SPH), where each particle carries physical properties like velocity, density, etc. To calculate the properties, we use the following ideas:

Radius is defined by us and helps limit the area of interaction (so only the particles in this area affect density and forces), made with a spatial map for checking the closest eight, plus the current grid (size defined by us) for calculations, not the entire window. 
Rest density is the target density where no pressure is created; smaller means particles sit farther apart. A particle's rest density influences the particle's pressure as defined by Pi = K(Pi – P0). Where P0 is the rest density.
Gas Constant or stiffness controls pressure scaling, large makes huge pressure forces, aka incompressible, small does the opposite. The gas constant is K in the above formula. 
Viscosity dampens interactions; higher means a "thicker" simulation. This metric makes faster-moving particle groups try and match those surrounding it by something out velocity, kind of a friction. 
Restitution controls how particles interact with boundaries, where lower means perfectly bounce.

## Example

https://github.com/user-attachments/assets/74b949e8-2215-4395-ae8e-80cf45fb3c81

## Acknowledgments

https://www.sci.utah.edu/publications/premoze03/ParticleFluidsHiRes.pdf
https://matthias-research.github.io/pages/publications/sca03.pdf
https://docs.oracle.com/javase/tutorial/uiswing/components/slider.html
