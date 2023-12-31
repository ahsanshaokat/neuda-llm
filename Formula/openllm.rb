# Generated by tools/update-brew-tap.py. DO NOT EDIT!
# Please refers to the original template file Formula/openllm.rb.j2
# vim: set ft=ruby:
class Openllm < Formula
  desc "OpenLLM: Operating LLMs in production"
  homepage "https://github.com/bentoml/OpenLLM"
  version "0.2.26"
  license "Apache-2.0"
  head "https://github.com/bentoml/OpenLLM, branch: main"
  url "https://github.com/bentoml/OpenLLM/archive/v0.2.26.tar.gz"
  sha256 "16f637e8025a553c50a40c436ecf0c83b696d4bacb5ce794d344be6ec34696fc"

  on_linux do
    url "https://github.com/bentoml/OpenLLM/releases/download/v0.2.26/openllm-0.2.26-x86_64-unknown-linux-musl.tar.gz"
    sha256 "f7906e66e95d8bb05b08077e09f170f5a27e95899f5396b8c1b4c07e5f4185b1"
  end
  on_macos do
    on_arm do
      url "https://github.com/bentoml/OpenLLM/releases/download/v0.2.26/openllm-0.2.26-aarch64-apple-darwin.tar.gz"
      sha256 "b16b816badb792f0944dfc469cbdba20e2744539c6286e40d54161697de5c40d"
    end
    on_intel do
      url "https://github.com/bentoml/OpenLLM/releases/download/v0.2.26/openllm-0.2.26-x86_64-apple-darwin.tar.gz"
      sha256 "8c166acb158b51875a73e0176d0c6f9303032fc4e810c64e06a7c220f98785ee"
    end
  end

  def install
    on_linux do
      bin.install "openllm-0.2.26-x86_64-unknown-linux-musl" => "openllm"
    end
  on_macos do
    on_arm do
      bin.install "openllm-0.2.26-aarch64-apple-darwin" => "openllm"
    end
    on_intel do
      bin.install "openllm-0.2.26-x86_64-apple-darwin" => "openllm"
    end
  end
    ohai "To get started, run: 'openllm --help'"
    ohai "To see supported models, run: 'openllm models'"
  end

  test do
    shell_output "#{bin}/openllm --version"
  end
end
