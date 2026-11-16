<p align="center">
  <img src="https://raw.githubusercontent.com/goome-ui/.github/main/profile/logo.png" alt="goome" width="150">
</p>

<h1 align="center">☕ java-goome ☕</h1>

<p align="center">
<b>Overlays on top of everything, normal desktop apps, or ui inside your game (Minecraft too), straight from Java.</b>
</p>

<p align="center">
  <img src="https://goome-ui.github.io/docs/assets/badges/java-17.svg" alt="Java 17+">
  <img src="https://goome-ui.github.io/docs/assets/badges/no-jni-glue.svg" alt="FFM + LWJGL">
  <img src="https://img.shields.io/badge/Linux-X11%20%2B%20Wayland-1b1d22?style=flat-square&logo=linux&logoColor=white" alt="Linux">
  <img src="https://img.shields.io/badge/Windows-10%20%2F%2011-1b1d22?style=flat-square&logo=windows11&logoColor=white" alt="Windows">
  <img src="https://img.shields.io/badge/macOS-14%2B-1b1d22?style=flat-square&logo=apple&logoColor=white" alt="macOS">
  <img src="https://img.shields.io/badge/license-GPL--3.0-1b1d22?style=flat-square" alt="GPL-3.0">
</p>

<p align="center">
  <a href="https://github.com/goome-ui/c-goome">goome</a> for Java. Draw see-through stuff over your whole screen, or open a regular window, or render into a texture for your game.<br>
  There's no JNI to build. Java 22+ talks to it with FFM, older Javas go through LWJGL, and a Fabric and NeoForge mod come with it.<br> <br>
  Do you like this project? If so, please let me know!
  <a href="https://sevvy-wevvy.com/discord">Discord</a>
</p>

<p align="center">
  <a href="https://goome-ui.github.io/docs/">Docs</a> &nbsp;·&nbsp;
  <a href="https://goome-ui.github.io/docs/builder/">UI Builder</a> &nbsp;·&nbsp;
  <a href="https://github.com/goome-ui/examples">Examples</a> &nbsp;·&nbsp;
  <a href="https://github.com/goome-ui/c-goome">C</a> &nbsp;·&nbsp;
  <a href="https://github.com/goome-ui/csharp-goome">C# + Unity</a> &nbsp;·&nbsp;
  <a href="https://github.com/goome-ui/python-goome">Python</a> &nbsp;·&nbsp;
  <a href="https://github.com/goome-ui/godot-goome">Godot</a>
</p>

<p align="center">
  To see what goome can do, grab one of the <a href="https://goome-ui.github.io/docs/downloads/">ready made apps</a>, like the HUD or the crosshair. To start your own, <code>goome new my-app --lang java</code> from <a href="https://github.com/goome-ui/goome-cli">goome-cli</a> makes it from a <a href="https://goome-ui.github.io/docs/templates/">template</a>.<br>
  Questions go in <a href="https://github.com/goome-ui/c-goome/discussions">Discussions</a>. If you want to help, the <a href="https://github.com/goome-ui/java-goome/labels/good%20first%20issue">good first issues</a> are small ones to start with.
</p>

<h3 align="center">🫥 An overlay on top of everything 🫥</h3>

```java
import io.github.goomeui.*;

try (Goome g = Goome.open("my overlay")) {
    boolean running = true;
    while (running) {
        for (Event e : g.events(16)) if (e.type() == Goome.QUIT) running = false;
        g.frame(0, c -> {
            c.roundRect(100, 100, 220, 60, 12, 0xE0202838);
            c.text(118, 120, "im on top of everything", 0xFFFFFFFF);
            c.hit(100, 100, 220, 60);
        });
    }
}
```

<h3 align="center">🪟 A normal app 🪟</h3>

```java
String[] name = {""};
new App("My app", 960, 640).onFrame(ui -> {
    name[0] = ui.input("Name", name[0], 64);
    if (ui.button("Say hi")) ui.toast("hi " + name[0], 2500);
}).run();
```

<h3 align="center">🎮 Inside a game engine 🎮</h3>

```java
Goome g = Goome.openOffscreen(800, 600, 1);
Ui ui = g.ui();
Window win = new Window(20, 20, 260, 0);
ByteBuffer tex = ByteBuffer.allocateDirect(800 * 600 * 4);
double volume = 0.5;
while (true) {
    ui.feed(Event.motion(mouseX, mouseY));            // your engines input goes in
    ui.begin(0);
    if (ui.window("Settings", win, Ui.WIN_MOVE)) volume = ui.slider("Volume", volume, 0, 1);
    ui.windowEnd();
    ui.end();
    Rect dirty = g.frameDirty();                       // null = nothing changed
    if (dirty != null) {
        g.frameCopy(tex, 800 * 4, PixelFormat.RGBA_PREMUL, dirty);
        uploadTexture(tex);
    }
    if (ui.wantsMouse()) { /* dont let the game see the mouse this frame */ }
}
```

<h3 align="center">⛏️ A Minecraft mod ⛏️</h3>

```java
mc.gui.setScreen(new GoomeScreen(Component.literal("my menu"), ui -> {
    if (ui.window("My mod", win, Ui.WIN_MOVE)) {
        fly = ui.toggle("Fly", fly);
        if (ui.button("Say hi")) ui.toast("hi from goome", 2000);
    }
    ui.windowEnd();
}));
```

<p align="center">
Overlay is still the default, nothing changes unless you ask for an app or offscreen. The mod part is down in the Minecraft section.
</p>

<details>
<summary align="center"><b>⬇️ Install ⬇️</b></summary>

<br>

<p align="center">
It's not on Maven Central. Grab <code>goome-0.5.0.jar</code> from the <a href="https://github.com/goome-ui/java-goome/releases">releases page</a> and drop it on your classpath, it has the native lib for every OS inside, both Macs too. The Fabric and NeoForge mod jars are on there as well.<br> <br>
In Gradle put it in <code>libs/</code>:
</p>

```sh
mkdir -p libs
curl -fsSL -o libs/goome-0.5.0.jar https://github.com/goome-ui/java-goome/releases/download/v0.5.0/goome-0.5.0.jar
```

```groovy
dependencies {
    implementation files('libs/goome-0.5.0.jar')
}
```

<p align="center">
For another version swap <code>0.5.0</code> for it, every one is on the releases page.<br> <br>
Building it yourself needs a C compiler and a JDK 25 for Gradle (the jar still runs on 17):
</p>

```sh
git clone https://github.com/goome-ui/java-goome
cd java-goome
./build.sh test      # builds build/natives/<os>/libgoome.so and runs the tests
./gradlew :goome:jar # goome/build/libs/goome-0.5.0.jar
```

<p align="center">
If you dont have a compiler, on Debian/Ubuntu thats <code>sudo apt install build-essential</code>, on Mac <code>xcode-select --install</code>, on Windows <a href="https://www.mingw-w64.org/">mingw-w64</a> gcc, run <code>./build.sh</code> from git bash. <code>CC</code> picks a different one.<br> <br>
The first time goome loads it unpacks the native lib into a cache folder (<code>~/.cache/goome/java</code>, <code>%LOCALAPPDATA%\goome\java</code> or <code>~/Library/Caches/goome/java</code>). <code>-Dgoome.cache=folder</code> moves that, and <code>-Dgoome.lib=/path/to/libgoome.so</code> skips it and uses your own lib.<br> <br>
On Java 22+ start it with <code>--enable-native-access=ALL-UNNAMED</code> or Java prints a warning (the jar manifest already says it for <code>java -jar</code>).
</p>
</details>

<details>
<summary align="center"><b>🔌 FFM Or LWJGL 🔌</b></summary>

<br>

<p align="center">
The api is the same both ways, only the way calls reach C changes.<br> <br>
Java 22 and up uses FFM (<code>java.lang.foreign</code>), which is in the JDK, and it picks that when it can.<br>
Java 17 and 21 use LWJGL's libffi calls, so you need <code>org.lwjgl:lwjgl</code> 3.3 or newer on the classpath. Minecraft ships LWJGL, so the mods just use the copy it already loaded.<br> <br>
Pick one by hand with <code>-Dgoome.native=ffm</code> or <code>-Dgoome.native=lwjgl</code>, <code>Goome.nativeKind()</code> says which one it got. The tests run against both on every OS.<br> <br>
64 bit only. goome itself is fine with 32 bit but these bindings assume 8 byte pointers.
</p>
</details>

<details>
<summary align="center"><b>⛏️ Minecraft ⛏️</b></summary>

<br>

<p align="center">
<code>minecraft/</code> has a Fabric mod and a NeoForge mod for Minecraft 26.3, built by CI on every push. Both do the same thing. <b>G</b> opens a little mod menu, and <b>H</b> toggles a hud in the corner.<br> <br>
The screen is an offscreen goome the size of the gui. Every frame goome draws into its own pixels, the changed part gets copied straight into a <code>NativeImage</code>, and that goes on screen as one premultiplied texture. All the input goes into goome first, typed text too, and while goome wants them (you're dragging a slider or typing in a box) Minecraft never sees them. When goome doesnt want them they go on to Minecraft like normal, so esc still closes the screen.<br> <br>
Since they go through LWJGL you don't get the native access warning either.<br> <br>
Making your own: <code>minecraft/common</code> is the part that doesnt touch Minecraft at all (<code>GoomeView</code> for input + texture upload, key maps, the sample <code>Menu</code>), <code>minecraft/shared</code> is the Minecraft side both loaders use (<code>GoomeScreen</code>, <code>GoomeHud</code>, <code>GoomeTexture</code>), and the loader folders are just the entrypoints. Swap <code>Menu::draw</code> for your own <code>Consumer&lt;Ui&gt;</code> like the one at the top, everything in the ui kit works in there.
</p>

```sh
./build.sh                                 # native lib for your os
cd minecraft/fabric && ../../gradlew build # build/libs/goome-fabric-0.5.0.jar
cd ../neoforge && ../../gradlew build      # build/libs/goome-neoforge-0.5.0.jar
```

<p align="center">
The CI jars have the native libs for every OS in them, the ones you build yourself only have yours.
</p>
</details>

<details>
<summary align="center"><b>🎛️ UI Kit 🎛️</b></summary>

<br>

<p align="center">
Its immediate mode like Dear ImGui. You call the widgets every frame, they hand back the new value, and <code>ui.changed()</code> says if the last one changed.
</p>

```java
try (Goome g = Goome.open("ui"); Ui ui = g.ui()) {
    Window win = new Window(40, 40, 320, 0);
    boolean on = false;
    double vol = 0.5;
    int tab = 0;
    while (true) {
        for (Event e : g.events(16)) ui.feed(e);
        if (ui.begin(0) == null) continue;
        if (ui.window("Settings", win, Ui.WIN_MOVE | Ui.WIN_COLLAPSE)) {
            tab = ui.tabs("tabs", tab, "Main", "More");
            on = ui.toggle("Enabled", on);
            vol = ui.slider("Volume", vol, 0, 1, "%.2f");
            if (ui.button("Save")) ui.toast("saved", 2000);
        }
        ui.windowEnd();
        ui.end();
    }
}
```

<p align="center">
Every widget in goome.h is here, down to tables, trees and the multiline <code>textEdit</code> and plots. The docs have the full list.<br> <br>
For a login box, <code>ui.password(label, text, cap)</code> shows stars and won't copy. <code>ui.inputEx(label, text, cap, flags, hint)</code> takes flags like <code>Ui.INPUT_ENTER_RETURNS</code> plus grey hint text, and <code>ui.entered()</code> tells you enter got pressed.<br> <br>
State that goome keeps a pointer to lives in small holder objects: <code>Window</code> (x y w h open), <code>Flag</code> (for menu checks, trees, modals), <code>Split</code> and <code>TextBuffer</code>.<br> <br>
<code>ui.theme()</code> is the look, see Changing The Look.<br> <br>
<code>ui.wantsMouse()</code> and <code>ui.wantsKeyboard()</code> tell you if the ui is using them right now, more on that in Taking The Mouse And Keys.
</p>
</details>

<details>
<summary align="center"><b>🖱️ Taking The Mouse And Keys 🖱️</b></summary>

<br>

<p align="center">
You mostly dont have to do anything. While youre dragging a slider, typing in a box or have a popup open, the ui takes the mouse and keyboard from the os by itself and gives them back after.<br> <br>
In an engine or a mod, ask it after <code>feed</code> or <code>end</code> and only pass the input on when goome doesnt want it:
</p>

```java
ui.feed(e);
if (!ui.wantsMouse()) game.mouse(e);
if (!ui.wantsKeyboard()) game.keys(e);
```

<p align="center">
<code>ui.wants()</code> gives back both bits (<code>Goome.WANT_MOUSE</code>, <code>Goome.WANT_KEYBOARD</code>) in one int.<br> <br>
To do it yourself, <code>ui.autoGrab(0)</code> turns the automatic part off, or pass just the bits you want it to keep handling. Then <code>g.grab(true)</code> takes focus and the pointer on an overlay until <code>g.grab(false)</code>, and <code>g.grabbed()</code> says if its on. In a window it keeps a drag going when the mouse leaves it.
</p>
</details>

<details>
<summary align="center"><b>🌈 Changing The Look 🌈</b></summary>

<br>

<p align="center">
Easiest is a preset, or <code>Theme.make(accent, dark, radius)</code> if you just want your own color. For one button, wrap it in a <code>ui.style()</code> try and it goes back when the block ends.
</p>

```java
ui.theme().set(Theme.preset(ui.theme(), Theme.PRESET_MIDNIGHT));
try (var s = ui.style().accent(0xFFFF6FA8).radius(0).push()) {
    if (ui.button("Delete")) deleteIt();
}
```

<p align="center">
The <a href="https://goome-ui.github.io/docs/">docs</a> cover the rest, from your own widgets with <code>ui.custom</code> to png icons and skins and the key map in <code>ui.config()</code>. <code>./gradlew :examples:runAnything</code> and <code>runCustom</code> show it off.
</p>
</details>

<details>
<summary align="center"><b>🎞️ Animations 🎞️</b></summary>

<br>

<p align="center">
<code>ui.anim(id, target, ms, ease)</code> gives back a number that slides over to the target, and it keeps its spot per id so you just call it every frame. <code>ui.animColor</code> does the same for a color. While anything is still moving <code>ui.animating()</code> is true, so your loop knows to keep drawing.
</p>

```java
Item it = ui.custom("card", 0, 80);
double lift = ui.anim("card lift", it.hovered(), 160, Theme.EASE_OUT);
int col = ui.animColor("card", it.held() ? 0xFFE0483E : 0xFF2D6CDF, 300, Theme.EASE_IN_OUT);
```

<p align="center">
The eases are <code>Theme.EASE_LINEAR</code>, <code>IN</code>, <code>OUT</code>, <code>IN_OUT</code>, <code>SMOOTH</code>, <code>BACK</code>, <code>SPRING</code> and <code>BOUNCE</code>. Spring and back overshoot a bit, thats on purpose. <code>Ui.ease(ease, t)</code> gives you the raw curve if you want it for your own stuff. <code>./gradlew :examples:runAnim</code> shows all of them.
</p>
</details>

<details>
<summary align="center"><b>🈶 Typing With An IME 🈶</b></summary>

<br>

<p align="center">
Inputs and <code>textEdit</code> turn the IME on by themselves while they have focus, so Japanese, Chinese, Korean and dead keys just work. The text being composed shows underlined at the caret and lands in the box when you commit it.<br> <br>
If you read events yourself, <code>Goome.TEXT</code> is finished text and <code>Goome.PREEDIT</code> is the stuff still being composed. <code>e.fullText()</code> has all of it (<code>e.msg()</code> gets cut at 63 bytes like in C) and <code>e.caret()</code> is the caret byte offset in the preedit. <code>g.textInput(on, caret)</code> turns it on for your own widget and puts the candidate box next to the caret.<br> <br>
In an engine, push them in like any other input: <code>ui.feed(Event.typed("日本"))</code> and <code>ui.feed(Event.preedit("にほ", 6))</code>.
</p>
</details>

<details>
<summary align="center"><b>🦮 Screen Readers 🦮</b></summary>

<br>

<p align="center">
The ui kit builds an accessibility tree from its widgets every frame. In a normal window it goes to the screen reader on its own (AT-SPI on linux, UI Automation on windows, NSAccessibility on mac), you dont have to do anything.<br> <br>
For your own <code>ui.custom</code> widgets call <code>ui.a11yRole(A11yNode.ROLE_BUTTON, "Knob")</code> and <code>ui.a11yValue("11")</code> right before it so it has a name. Offscreen and overlays keep it off, turn it on with <code>ui.a11y(true)</code>, then <code>ui.a11yTree()</code> gives you a <code>List&lt;A11yNode&gt;</code> (role, label, value, parent, states, where it is) to hand to your engine. <code>ui.a11yPress(node)</code>, <code>a11ySet</code>, <code>a11yExpand</code> and <code>a11yFocus</code> do what a screen reader would.
</p>
</details>

<details>
<summary align="center"><b>😀 Emoji And RTL 😀</b></summary>

<br>

<p align="center">
Color emoji draw right (COLR, CBDT and sbix fonts), with skin tones, ZWJ sequences and flags. <code>Font.system</code> already adds the system emoji font, and <code>font.fallback(other)</code> adds your own fonts to try for letters the first one doesnt have.<br> <br>
Arabic and Hebrew lay out right to left, Arabic letters join up, and the caret and selection move the way you'd expect in mixed text. <code>Font.textDir(Font.DIR_RTL)</code> forces the direction, <code>DIR_AUTO</code> (the default) goes by the first strong letter of each line. <code>./gradlew :examples:runText</code> has a bunch of it.
</p>
</details>

<details>
<summary align="center"><b>📚 The API 📚</b></summary>

<br>

<p align="center">
<code>Goome</code> is the handle you open (overlay, app or offscreen) and it does events and hotkeys, plus grabs and the window itself.<br>
<code>Canvas</code> draws.<br>
<code>Image</code> holds a screen grab, <code>Bitmap</code> a loaded picture and <code>Font</code> a font.<br>
<code>Ui</code> is the ui kit, and <code>App</code> wraps it in a ready made loop if you don't want to write one.<br> <br>
Anything holding native memory is <code>AutoCloseable</code> and closing twice is fine. Using something after its gone (a canvas after <code>end</code>, a grab after a newer grab, a closed font) throws <code>GoomeException.Gone</code>, it doesnt crash. Errors from C come out as <code>GoomeException</code> with the goome code, and a subclass per code (<code>NotSupported</code>, <code>Again</code>, <code>BadArg</code>...).
</p>
</details>

<details>
<summary align="center"><b>🧩 Examples 🧩</b></summary>

<br>

<p align="center">
<code>examples/</code> has ten of them, from Hello up to UiDemo and Notes, plus Anim and Text for the newer stuff. Most are ports of the C examples with the same name.<br>
Offscreen is the odd one, it fakes the input and saves a png with no window open.
</p>

```sh
./gradlew :examples:runUiDemo
./gradlew :examples:runGrab -Pargs="0 out.ppm"
```

<p align="center">
Bigger projects are in <a href="https://github.com/goome-ui/examples">goome-ui/examples</a>, like hosts for raylib, SDL, LÖVE, plus native mods. The <a href="https://goome-ui.github.io/docs/">docs</a> have every function, with the C, C#, Python and Godot versions next to it.
</p>
</details>

<details>
<summary align="center"><b>🖥️ Where It Works 🖥️</b></summary>

<br>

<p align="center">
Wherever goome works, its the same C code underneath. The table of what each desktop can do (Wayland, X11, GNOME, Windows, macOS) is in the <a href="https://github.com/goome-ui/c-goome#readme">c-goome readme</a>. <code>g.caps()</code> / <code>g.can(Goome.CAP_HOTKEYS)</code> tells you at runtime.
</p>
</details>

<details>
<summary align="center"><b>🧪 Testing Without A Screen 🧪</b></summary>

<br>

<p align="center">
goome has a <code>test</code> backend that fakes a screen and plays a script of what the "user" does. All the tests here run on it or on offscreen, no display needed:
</p>

```java
Goome g = Goome.open(new Options().backend(Goome.TEST)
    .testScript("screen 800x600 1; 2 click 100,100; 4 key hi; end 8")
    .testShots("shots"));
```

<p align="center">
Or set <code>GOOME_BACKEND=test</code> and <code>GOOME_TEST_SCRIPT=...</code> on any program. The draw/image/font/ui tests check the same pixel hashes as c-goome's own tests, and CI runs the Hello and UiDemo examples next to the C ones and checks every frame is byte for byte the same.
</p>
</details>

<details>
<summary align="center"><b>🔧 Updating goome 🔧</b></summary>

<br>

<p align="center">
<code>native/goome.h</code> is a copy of c-goome's single header, the commit it came from is in <code>native/VERSION</code>.
</p>

```sh
tools/update_native.sh                 # newest c-goome main
tools/update_native.sh ../c-goome dev  # or a local checkout and branch
./build.sh test
```
</details>

<details>
<summary align="center"><b>🛠 Pull Requests 🛠</b></summary>

<p align="center">
Want to help with java-goome? Make A pull request to <b>dev</b>!<br>
Pull request allow for anyone to request a change to this code. In turn meaning that <b>YOU</b> can contribute to this project.<br>
As long as it works, and doesnt vary off too far from the main idea its very likely it get accepted! Read <a href="CONTRIBUTING.md">CONTRIBUTING.md</a> first, theres a bot that closes PRs that break the rules.
</p>
</details>

<details>
<summary align="center"><b>⚖️ License ⚖️</b></summary>

<br>

<p align="center">
java-goome is under the <a href="LICENSE">GPL-3.0</a>.<br> <br>
<sub>
• You can use it, change it and share it<br>
• If you share something made with it, you have to share your source too, under the same license<br>
• Keep the credits
</sub><br> <br>
In short: play fair, keep it open, and give credit where it's due.
</p>
</details>

<details>
<summary align="center"><b>🩷 Donate 🩷</b></summary>

<br>

<p align="center">
<a href="https://sevvy-wevvy.com/kofi">Ko-fi</a> (Preferred)<br>
<a href="https://sevvy-wevvy.com/patreon">Patreon</a><br>
<a href="https://sevvy-wevvy.com/throne">Throne</a>
</p>
</details>

<p align="center">
<b>🫶 Contributors 🫶</b><br>
Thanks to everyone who helped out!<br> <br>
<a href="https://github.com/goome-ui/java-goome/graphs/contributors"><img src="https://contrib.rocks/image?repo=goome-ui/java-goome" alt="contributors"></a>
</p>
