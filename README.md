# Simple Pad

Lightweight text editor with Batch syntax highlighting.

Licensed under the [MIT License](LICENSE). See [Terms of Service](TERMS_OF_SERVICE.md).

## Install on Linux Mint

```bash
curl -fsSL https://klcenka9.github.io/simple-pad/simple-pad-archive-keyring.gpg | sudo tee /usr/share/keyrings/simple-pad-archive-keyring.gpg >/dev/null
echo 'deb [signed-by=/usr/share/keyrings/simple-pad-archive-keyring.gpg] https://klcenka9.github.io/simple-pad stable main' | sudo tee /etc/apt/sources.list.d/simple-pad.list
sudo apt update
sudo apt install simple-pad
```

Run it from the application menu or with `simple-pad`.

## Terminal version

```bash
simple-pad-cli example.bat
```

Use `/help` inside the editor to see the available commands.

## Build a package

```bash
./build-deb.sh
```

## Publish the APT repository

```bash
./build-apt-repo.sh
```
