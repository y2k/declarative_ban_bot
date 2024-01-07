"use strict";
import { promises as fs } from 'fs';
import m from './sample4.js';
const runtime_create_world = () => { return { "perform": (name, args) => { return console.error("Effect not handled:", ("" + "[" + name + "]"), args) } } }
const runtime_attach_effect_handler = (world, keyf, f) => { return { ...world, perform: (key, args) => { return (key == keyf) ? (f(globalThis, args)) : (world.perform(key, args)) } } }
m.fetch({ "json": () => { return fs.readFile("../test/samples/sample1.json").then(JSON.parse) } }, process.env, runtime_attach_effect_handler(runtime_create_world(), "fetch", (js, args) => { return console.info("[INFO]", "[fetch]", args) }))
