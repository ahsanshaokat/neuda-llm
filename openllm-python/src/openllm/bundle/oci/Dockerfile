# syntax=docker/dockerfile-upstream:master
# Adapted from: https://github.com/pytorch/pytorch/blob/master/Dockerfile
FROM debian:bullseye-slim as pytorch-install

ARG PYTORCH_VERSION=2.0.1
ARG PYTHON_VERSION=3.9
ARG CUDA_VERSION=11.8
ARG MAMBA_VERSION=23.1.0-1
ARG CUDA_CHANNEL=nvidia
ARG INSTALL_CHANNEL=pytorch

# Automatically set by buildx
ARG TARGETPLATFORM

ENV PATH /opt/conda/bin:$PATH

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        ccache \
        curl \
        git && \
        rm -rf /var/lib/apt/lists/*

# Install conda
# translating Docker's TARGETPLATFORM into mamba arches
RUN <<EOT
case ${TARGETPLATFORM} in
    "linux/arm64")  MAMBA_ARCH=aarch64  ;;
    *)              MAMBA_ARCH=x86_64   ;;
esac
curl -fsSL -v -o ~/mambaforge.sh -O  "https://github.com/conda-forge/miniforge/releases/download/${MAMBA_VERSION}/Mambaforge-${MAMBA_VERSION}-Linux-${MAMBA_ARCH}.sh"
EOT

RUN <<EOT
chmod +x ~/mambaforge.sh
bash ~/mambaforge.sh -b -p /opt/conda
rm ~/mambaforge.sh
EOT


# Install pytorch
# On arm64 we exit with an error code
RUN <<EOT
case ${TARGETPLATFORM} in
    "linux/arm64")  exit 1 ;;
    *)              /opt/conda/bin/conda update -y conda &&  /opt/conda/bin/conda install -c "${INSTALL_CHANNEL}" -c "${CUDA_CHANNEL}" -y "python=${PYTHON_VERSION}" pytorch==$PYTORCH_VERSION "pytorch-cuda=$(echo $CUDA_VERSION | cut -d'.' -f 1-2)"  ;;
esac
/opt/conda/bin/conda clean -ya
EOT

# CUDA kernels builder image
FROM pytorch-install as kernel-builder

RUN apt-get update && apt-get install -y --no-install-recommends ninja-build && \
    rm -rf /var/lib/apt/lists/*

RUN /opt/conda/bin/conda install -c "nvidia/label/cuda-11.8.0"  cuda==11.8 && \
    /opt/conda/bin/conda clean -ya

# NOTE: Build vllm CUDA kernels
FROM kernel-builder as vllm-builder

ENV COMMIT_HASH d1744376ae9fdbfa6a2dc763e1c67309e138fa3d
ARG COMMIT_HASH=${COMMIT_HASH}

WORKDIR /usr/src

RUN <<EOT
git clone https://github.com/vllm-project/vllm.git && cd vllm
git fetch && git checkout ${COMMIT_HASH}
python setup.py build
EOT

# NOTE: Build flash-attention-2 CUDA kernels
FROM kernel-builder as flash-attn-v2-builder

ENV COMMIT_HASH 4c98d0b41f38ee638a979064856ae06fc1aec8b6
ARG COMMIT_HASH=${COMMIT_HASH}

WORKDIR /usr/src

RUN <<EOT
pip install packaging
git clone https://github.com/Dao-AILab/flash-attention.git flash-attention-v2 && cd flash-attention-v2
git fetch && git checkout ${COMMIT_HASH}
python setup.py build
EOT

# NOTE: Build auto-gptq CUDA kernels
FROM kernel-builder as auto-gptq-builder

ENV COMMIT_HASH 18326851213568df3c5bbbb1169fe51c7f7d6c60
ARG COMMIT_HASH=${COMMIT_HASH}

WORKDIR /usr/src

RUN <<EOT
pip install packaging
git clone https://github.com/PanQiWei/AutoGPTQ.git && cd AutoGPTQ
git fetch && git checkout ${COMMIT_HASH}
TORCH_CUDA_ARCH_LIST="7.5;8.0;8.6+PTX;8.9;9.0" python setup.py build
EOT

# base image
FROM nvidia/cuda:11.8.0-cudnn8-runtime-ubuntu22.04 as base-container

# Conda env
ENV PATH=/opt/conda/bin:$PATH \
    CONDA_PREFIX=/opt/conda

WORKDIR /usr/src

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
        libssl-dev ca-certificates make && \
    rm -rf /var/lib/apt/lists/*

# Copy conda with PyTorch installed
COPY --from=pytorch-install /opt/conda /opt/conda

# Copy build artefacts for vllm
COPY --from=vllm-builder /usr/src/vllm/build/lib.linux-x86_64-cpython-39 /opt/conda/lib/python3.9/site-packages

# Copy build artefacts for flash-attention-v2
COPY --from=flash-attn-v2-builder /usr/src/flash-attention-v2/build/lib.linux-x86_64-cpython-39 /opt/conda/lib/python3.9/site-packages

# Copy build artefacts for auto-gptq
COPY --from=auto-gptq-builder /usr/src/AutoGPTQ/build/lib.linux-x86_64-cpython-39 /opt/conda/lib/python3.9/site-packages

# Install required dependencies
COPY openllm-python/src src
COPY hatch.toml README.md CHANGELOG.md openllm-python/pyproject.toml ./

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        ccache \
        curl \
        git && \
        rm -rf /var/lib/apt/lists/*

# Install all required dependencies
RUN --mount=type=cache,target=/root/.cache/pip \
  pip install --extra-index-url "https://download.pytorch.org/whl/cu118" -v --no-cache-dir \
    "ray==2.6.0" "einops" "torch>=2.0.1+cu118" xformers "jax[cuda11_local]" -f https://storage.googleapis.com/jax-releases/jax_cuda_releases.html ".[opt,mpt,fine-tune,llama,chatglm]"

FROM base-container

ENTRYPOINT ["python3", "-m", "openllm"]
